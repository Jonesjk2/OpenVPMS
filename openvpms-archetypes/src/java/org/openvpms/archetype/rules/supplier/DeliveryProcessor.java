/*
 * Version: 1.0
 *
 * The contents of this file are subject to the OpenVPMS License Version
 * 1.0 (the 'License'); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.openvpms.org/license/
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * Copyright 2014 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.rules.supplier;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.AndPredicate;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.openvpms.archetype.rules.act.ActStatus;
import org.openvpms.archetype.rules.act.ActStatusHelper;
import org.openvpms.archetype.rules.math.Currencies;
import org.openvpms.archetype.rules.math.MathRules;
import org.openvpms.archetype.rules.practice.PracticeRules;
import org.openvpms.archetype.rules.product.ProductPriceRules;
import org.openvpms.archetype.rules.product.ProductPriceUpdater;
import org.openvpms.archetype.rules.product.ProductRules;
import org.openvpms.archetype.rules.product.ProductSupplier;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.act.ActRelationship;
import org.openvpms.component.business.domain.im.act.FinancialAct;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.common.IMObjectRelationship;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.domain.im.product.ProductPrice;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.functor.RefEquals;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.DescriptorHelper;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.business.service.archetype.helper.TypeHelper;
import org.openvpms.component.business.service.lookup.ILookupService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openvpms.component.business.service.archetype.functor.IsActiveRelationship.isActiveNow;


/**
 * Processes <em>POSTED</em> deliveries and returns.
 * <p/>
 * For each item in a delivery/return, updates:
 * <ol>
 * <li>the <em>receivedQuantity</em> node of the associated order (if any)</li>
 * <li>the <em>entityRelationship.productSupplier</em> associated with
 * the product and supplier, if the item is a delivery</li>
 * <li>the <em>quantity</em> node of the
 * <em>entityRelationship.productStockLocation</em> associated with the
 * product and stock location</li>
 * </ol>
 * If an order item changes status, the delivery status of the parent
 * order is then re-evaluated.
 * <p/>
 * For each delivery item with a <em>batchNumber</em> or <em>expiryDate</em>, determines if an
 * <em>entity.productBatch</em> exists. If not, one will be created.
 *
 * @author Tim Anderson
 */
public class DeliveryProcessor {

    /**
     * The delivery/return act.
     */
    private final Act act;

    /**
     * Product rules.
     */
    private final ProductRules rules;

    /**
     * The set of objects to save on completion.
     */
    private Set<IMObject> toSave = new LinkedHashSet<IMObject>();

    /**
     * Cache of orders that need to have their delivery statuses re-evaluated.
     */
    Map<IMObjectReference, Act> orders = new HashMap<IMObjectReference, Act>();

    /**
     * Cache of order item statuses.
     */
    Map<IMObjectReference, DeliveryStatus> statuses
            = new HashMap<IMObjectReference, DeliveryStatus>();

    /**
     * The archetype service.
     */
    private final IArchetypeService service;

    /**
     * Product price updater.
     */
    private final ProductPriceUpdater priceUpdater;

    /**
     * The supplier.
     */
    private Party supplier;

    /**
     * The stock location.
     */
    private Party stockLocation;


    /**
     * Constructs an {@link DeliveryProcessor}.
     *
     * @param act        the delivery/return act
     * @param service    the archetype service
     * @param currencies the currency cache
     * @param lookups    the lookup service
     */
    public DeliveryProcessor(Act act, IArchetypeService service, Currencies currencies, ILookupService lookups) {
        this.act = act;
        this.service = service;
        this.rules = new ProductRules(service);
        ProductPriceRules priceRules = new ProductPriceRules(service, lookups);
        PracticeRules practiceRules = new PracticeRules(service, currencies);
        priceUpdater = new ProductPriceUpdater(priceRules, practiceRules, service);
    }

    /**
     * Applies changes, if the act is POSTED and hasn't already been posted.
     *
     * @throws ArchetypeServiceException  for any archetype service error
     * @throws DeliveryProcessorException if an item is missing a product
     */
    public void apply() {
        if (ActStatus.POSTED.equals(act.getStatus()) && !ActStatusHelper.isPosted(act, service)) {
            ActBean bean = new ActBean(act, service);
            supplier = (Party) bean.getNodeParticipant("supplier");
            stockLocation = (Party) bean.getNodeParticipant("stockLocation");
            for (Act item : bean.getNodeActs("items")) {
                processItem(item);
            }

            // for each order that has order items that have changed, update
            // their delivery status where required
            for (Act order : orders.values()) {
                updateDeliveryStatus(order);
            }

            if (!toSave.isEmpty()) {
                service.save(toSave);
            }
        }
    }

    /**
     * Returns the delivery status of an <em>act.supplierOrderItem</em>.
     *
     * @param orderItem the order item
     * @param service   the archetype service
     * @return the delivery status of the order item
     */
    public static DeliveryStatus getDeliveryStatus(FinancialAct orderItem,
                                                   IArchetypeService service) {
        IMObjectBean bean = new IMObjectBean(orderItem, service);
        BigDecimal quantity = bean.getBigDecimal("quantity", BigDecimal.ZERO);
        BigDecimal received = bean.getBigDecimal("receivedQuantity", BigDecimal.ZERO);
        BigDecimal cancelled = bean.getBigDecimal("cancelledQuantity", BigDecimal.ZERO);
        return DeliveryStatus.getStatus(quantity, received, cancelled);
    }

    /**
     * Processes a delivery/return item.
     *
     * @param item the delivery/return item
     * @throws ArchetypeServiceException  for any archetype service error
     * @throws DeliveryProcessorException if the item is missing a product
     */
    private void processItem(Act item) {
        ActBean itemBean = new ActBean(item, service);
        BigDecimal receivedQuantity = itemBean.getBigDecimal("quantity");
        int receivedPackSize = itemBean.getInt("packageSize");
        boolean delivery = TypeHelper.isA(act, SupplierArchetypes.DELIVERY);
        if (!delivery) {
            receivedQuantity = receivedQuantity.negate();
        }
        Product product = (Product) itemBean.getNodeParticipant("product");
        if (product == null) {
            throw new DeliveryProcessorException(DeliveryProcessorException.ErrorCode.NoProduct,
                                                 DescriptorHelper.getDisplayName(item, service), item.getId(),
                                                 DescriptorHelper.getDisplayName(act, service), act.getId());
        }

        // update the associated order's received quantity
        for (Act orderItem : itemBean.getNodeActs("order")) {
            updateReceivedQuantity((FinancialAct) orderItem, receivedQuantity, receivedPackSize);
        }

        if (delivery) {
            // update the product-supplier relationship
            if (supplier != null) {
                updateProductSupplier(product, itemBean);
            }
            String batchNumber = StringUtils.trimToNull(itemBean.getString("batchNumber"));
            Date expiryDate = itemBean.getDate("expiryDate");
            IMObjectReference manufacturer = itemBean.getNodeParticipantRef("manufacturer");
            if (batchNumber != null || expiryDate != null) {
                updateBatch(product, batchNumber, expiryDate, manufacturer);
            }
        }

        // update the stock quantity for the product at the stock location
        if (stockLocation != null) {
            updateStockQuantity(product, stockLocation, receivedQuantity, receivedPackSize);
        }
    }

    /**
     * Creates a batch if one doesn't exist, or updates an existing batch if required.
     *
     * @param product      the product
     * @param batchNumber  the batch number. May be {@code null}
     * @param expiryDate   the expiry date. May be {@code null}
     * @param manufacturer the product manufacturer. May be {@code null}
     */
    private void updateBatch(Product product, String batchNumber, Date expiryDate, IMObjectReference manufacturer) {
        IMObjectReference productRef = product.getObjectReference();
        List<Entity> batches = rules.getBatches(productRef, batchNumber, expiryDate, manufacturer);
        if (batches.isEmpty()) {
            if (batchNumber == null) {
                batchNumber = product.getName();
            }
            if (!StringUtils.isEmpty(batchNumber)) {
                Entity batch = rules.createBatch(productRef, batchNumber, expiryDate, manufacturer);
                updateBatchLocation(batch);
                toSave.add(batch);
            }
        } else {
            Entity batch = batches.get(0);
            if (updateBatchLocation(batch)) {
                toSave.add(batch);
            }
        }
    }

    /**
     * Updates a batch with the stock location, if required.
     *
     * @param batch the batch
     * @return {@code true} if the location was added
     */
    private boolean updateBatchLocation(Entity batch) {
        EntityBean bean = new EntityBean(batch, service);
        if (!bean.getNodeTargetObjectRefs("stockLocations").contains(stockLocation.getObjectReference())) {
            bean.addNodeTarget("stockLocations", stockLocation);
            return true;
        }
        return false;
    }

    /**
     * Updates the quantity of a product at a stock location.
     * <p/>
     * If no <em>entityRelationship.productStockLocation</em> exists for the
     * product and stock  location, one will be created.
     *
     * @param product       the product
     * @param stockLocation the stock location
     * @param quantity      the quantity received
     * @param packageSize   the size of the package
     * @throws ArchetypeServiceException for any archetype service error
     */
    private void updateStockQuantity(Product product, Party stockLocation,
                                     BigDecimal quantity, int packageSize) {
        EntityBean bean = new EntityBean(product, service);
        if (bean.hasNode("stockLocations")) {
            Predicate predicate = AndPredicate.getInstance(isActiveNow(), RefEquals.getTargetEquals(stockLocation));
            IMObjectRelationship relationship = bean.getNodeRelationship("stockLocations", predicate);
            if (relationship == null) {
                relationship = bean.addRelationship("entityRelationship.productStockLocation", stockLocation);
                toSave.add(product);
                toSave.add(stockLocation);
            } else {
                toSave.add(relationship);
            }
            BigDecimal units
                    = quantity.multiply(BigDecimal.valueOf(packageSize));
            IMObjectBean relBean = new IMObjectBean(relationship, service);
            BigDecimal stockQuantity = relBean.getBigDecimal("quantity");
            stockQuantity = stockQuantity.add(units);
            if (stockQuantity.compareTo(BigDecimal.ZERO) < 0) {
                stockQuantity = BigDecimal.ZERO;
            }
            relBean.setValue("quantity", stockQuantity);
        }
    }

    /**
     * Updates the <em>receivedQuantity</em> node of an order item.
     *
     * @param orderItem   the order item
     * @param quantity    the quantity in the delivery/return. If return, the
     *                    quantity will be negative
     * @param packageSize the size of the package
     * @throws ArchetypeServiceException for any archetype service error
     */
    private void updateReceivedQuantity(FinancialAct orderItem,
                                        BigDecimal quantity,
                                        int packageSize) {
        ActBean bean = new ActBean(orderItem, service);
        BigDecimal received = bean.getBigDecimal(
                "receivedQuantity");
        BigDecimal cancelled = bean.getBigDecimal("cancelledQuantity");
        BigDecimal orderQuantity = orderItem.getQuantity();
        DeliveryStatus oldStatus = DeliveryStatus.getStatus(orderQuantity,
                                                            received,
                                                            cancelled);

        int orderedPackSize = bean.getInt("packageSize");
        if (packageSize != orderedPackSize && orderedPackSize != 0) {
            // need to convert the quantity to the order package quantity
            quantity = quantity.multiply(BigDecimal.valueOf(packageSize));
            quantity = MathRules.divide(quantity, orderedPackSize, 3);
        }
        received = received.add(quantity);
        if (received.compareTo(BigDecimal.ZERO) < 0) {
            received = BigDecimal.ZERO;
        }
        bean.setValue("receivedQuantity", received);
        toSave.add(orderItem);

        DeliveryStatus newStatus = DeliveryStatus.getStatus(orderQuantity,
                                                            received,
                                                            cancelled);

        // cache the order item's delivery status
        statuses.put(orderItem.getObjectReference(), newStatus);
        if (oldStatus != newStatus) {
            // the order item's status has changed, so need to cache the
            // associated order in order to determine if its delivery status
            // needs to be updated once the delivery/return has been processed
            loadOrder(bean);
        }
    }

    /**
     * Updates the delivery status of an order, if required.
     *
     * @param order the order
     * @throws ArchetypeServiceException for any archetype service error
     */
    private void updateDeliveryStatus(Act order) {
        ActBean bean = new ActBean(order, service);
        DeliveryStatus current = DeliveryStatus.valueOf(bean.getString("deliveryStatus"));
        DeliveryStatus newStatus = null;
        boolean pending = false;
        boolean part = false;
        boolean full = false;
        for (ActRelationship relationship : bean.getRelationships(SupplierArchetypes.ORDER_ITEM_RELATIONSHIP)) {
            IMObjectReference target = relationship.getTarget();
            DeliveryStatus status = getDeliveryStatus(target);
            if (status == DeliveryStatus.PART) {
                part = true;
                break;
            } else if (status == DeliveryStatus.FULL) {
                full = true;
            } else {
                pending = true;
            }
        }
        if (part || (pending && full)) {
            newStatus = DeliveryStatus.PART;
        } else if (!pending && full) {
            newStatus = DeliveryStatus.FULL;
        } else if (pending) {
            newStatus = DeliveryStatus.PENDING;
        }
        if (newStatus != null && newStatus != current) {
            bean.setValue("deliveryStatus", newStatus.toString());
            toSave.add(order);
        }
    }

    /**
     * Returns the delivery status of an order item.
     *
     * @param ref a reference to the order item. May be <tt>null</tt>
     * @return the delivery staus of the item, or <tt>null</tt> if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    private DeliveryStatus getDeliveryStatus(IMObjectReference ref) {
        DeliveryStatus result = null;
        if (ref != null) {
            result = statuses.get(ref);
            if (result == null) {
                FinancialAct item = getAct(ref);
                if (item != null) {
                    result = getDeliveryStatus(item, service);
                }
            }
        }
        return result;
    }

    /**
     * Retrieves the order associated with an order item.
     * <p/>
     * The order is cached in {@link #orders}.
     *
     * @param bean the bean wrapping the order item
     * @return the order associated with the item, or <tt>null</tt> if none is
     *         found
     * @throws ArchetypeServiceException for any archetype service error
     */
    private Act loadOrder(ActBean bean) {
        Act order = null;
        List<ActRelationship> relationships
                = bean.getRelationships(
                SupplierArchetypes.ORDER_ITEM_RELATIONSHIP);
        if (!relationships.isEmpty()) {
            IMObjectReference ref = relationships.get(0).getSource();
            order = orders.get(ref);
            if (order == null) {
                order = getAct(ref);
                if (order != null) {
                    orders.put(ref, order);
                }
            }
        }
        return order;
    }

    /**
     * Retrieves an act given its reference.
     *
     * @param ref the act reference
     * @return the act, or <tt>null</tt> if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    private FinancialAct getAct(IMObjectReference ref) {
        FinancialAct act = null;
        if (ref != null) {
            act = (FinancialAct) service.get(ref);
        }
        return act;
    }

    /**
     * Updates an <em>entityRelationship.productSupplier</em> from an
     * <em>act.supplierDeliveryItem</em>, if required.
     *
     * @param product          the product
     * @param deliveryItemBean a bean wrapping the delivery item
     */
    private void updateProductSupplier(Product product,
                                       ActBean deliveryItemBean) {
        int size = deliveryItemBean.getInt("packageSize");
        String units = deliveryItemBean.getString("packageUnits");
        String reorderCode = deliveryItemBean.getString("reorderCode");
        String reorderDesc = deliveryItemBean.getString("reorderDescription");
        BigDecimal listPrice = deliveryItemBean.getBigDecimal("listPrice");
        BigDecimal nettPrice = deliveryItemBean.getBigDecimal("unitPrice");
        ProductSupplier ps = rules.getProductSupplier(product, supplier, reorderCode, size, units);
        boolean save = true;
        if (ps == null) {
            // no product-supplier relationship, so create a new one
            ps = rules.createProductSupplier(product, supplier);
            ps.setPackageSize(size);
            ps.setPackageUnits(units);
            ps.setReorderCode(reorderCode);
            ps.setReorderDescription(reorderDesc);
            ps.setListPrice(listPrice);
            ps.setNettPrice(nettPrice);
            ps.setPreferred(true);
        } else if (size != ps.getPackageSize()
                   || !ObjectUtils.equals(units, ps.getPackageUnits())
                   || !MathRules.equals(listPrice, ps.getListPrice())
                   || !MathRules.equals(nettPrice, ps.getNettPrice())
                   || !ObjectUtils.equals(ps.getReorderCode(), reorderCode)
                   || !ObjectUtils.equals(ps.getReorderDescription(), reorderDesc)) {
            // properties are different to an existing relationship
            ps.setPackageSize(size);
            ps.setPackageUnits(units);
            ps.setReorderCode(reorderCode);
            ps.setReorderDescription(reorderDesc);
            ps.setListPrice(listPrice);
            ps.setNettPrice(nettPrice);
        } else {
            save = false;
        }
        if (ps.isAutoPriceUpdate() && supplier.isActive()) {
            // only update prices if the supplier is active
            updateUnitPrices(product, ps);
        }

        if (save) {
            toSave.add(ps.getRelationship());
        }
    }

    /**
     * Recalculates the cost node of any <em>productPrice.unitPrice</em>
     * associated with the product.
     *
     * @param product         the product
     * @param productSupplier the product supplier
     */
    private void updateUnitPrices(Product product,
                                  ProductSupplier productSupplier) {
        List<ProductPrice> prices
                = priceUpdater.update(product, productSupplier, false);
        toSave.addAll(prices);
    }


}


