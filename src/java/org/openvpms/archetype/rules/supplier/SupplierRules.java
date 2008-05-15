/*
 *  Version: 1.0
 *
 *  The contents of this file are subject to the OpenVPMS License Version
 *  1.0 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.openvpms.org/license/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  Copyright 2007 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.archetype.rules.supplier;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.AndPredicate;
import org.apache.commons.lang.ObjectUtils;
import org.openvpms.component.business.domain.im.common.EntityRelationship;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.functor.IsActiveRelationship;
import org.openvpms.component.business.service.archetype.functor.RefEquals;
import org.openvpms.component.business.service.archetype.helper.EntityBean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Supplier rules.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class SupplierRules {

    /**
     * The archetype service.
     */
    private final IArchetypeService service;


    /**
     * Constructs a new <tt>SupplierRules</tt>.
     *
     * @throws ArchetypeServiceException if the archetype service is not
     *                                   configured
     */
    public SupplierRules() {               
        this(ArchetypeServiceHelper.getArchetypeService());
    }

    /**
     * Construct a new <tt>SupplierRules/code>.
     *
     * @param service the archetype service
     */
    public SupplierRules(IArchetypeService service) {
        this.service = service;
    }

    /**
     * Returhs the referral vet practice for a vet overlapping the specified
     * time.
     *
     * @param vet  the vet
     * @param time the time
     * @return the practice the vet is associated with or <tt>null</tt> if
     *         the vet is not associated with any practice for the time frame
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Party getReferralVetPractice(Party vet, Date time) {
        EntityBean bean = new EntityBean(vet, service);
        return (Party) bean.getNodeSourceEntity("practices", time);
    }

    /**
     * Determines if a supplier supplies a particular product.
     *
     * @param supplier the supplier
     * @param product  the product
     */
    public boolean isSuppliedBy(Party supplier, Product product) {
        EntityBean bean = new EntityBean(supplier, service);
        Predicate predicate = AndPredicate.getInstance(
                IsActiveRelationship.ACTIVE_NOW,
                RefEquals.getSourceEquals(product));
        return bean.getNodeRelationship("products", predicate) != null;
    }

    /**
     * Returns all active <em>entityRelationship.productSupplier</em>
     * relationships for a particular supplier.
     *
     * @param supplier the supplier
     * @return the relationships, wrapped in {@link ProductSupplier} instances
     */
    public List<ProductSupplier> getProductSuppliers(Party supplier) {
        List<ProductSupplier> result = new ArrayList<ProductSupplier>();
        EntityBean bean = new EntityBean(supplier, service);
        List<EntityRelationship> relationships
                = bean.getNodeRelationships("products",
                                            IsActiveRelationship.ACTIVE_NOW);
        for (EntityRelationship relationship : relationships) {
            result.add(new ProductSupplier(relationship, service));
        }
        return result;
    }

    /**
     * Returns all active <em>entityRelationship.productSupplier</em>
     * relationships for a particular supplier and product.
     *
     * @param supplier the supplier
     * @param product  the product
     * @return the relationships, wrapped in {@link ProductSupplier} instances
     */
    public List<ProductSupplier> getProductSuppliers(Party supplier,
                                                     Product product) {
        List<ProductSupplier> result = new ArrayList<ProductSupplier>();
        EntityBean bean = new EntityBean(supplier, service);
        Predicate predicate = AndPredicate.getInstance(
                IsActiveRelationship.ACTIVE_NOW,
                RefEquals.getSourceEquals(product));
        List<EntityRelationship> relationships
                = bean.getNodeRelationships("products", predicate);
        for (EntityRelationship relationship : relationships) {
            result.add(new ProductSupplier(relationship, service));
        }
        return result;
    }

    /**
     * Returns an <em>entityRelationship.productSupplier</em> relationship
     * for a supplier, product and package size and units.
     * <p/>
     * If there is a match on supplier and product, but no match on package
     * size, but there is a relationship where the size is <tt>0</tt>, then
     * this will be returned.
     *
     * @param supplier     the supplier
     * @param product      the product
     * @param packageSize  the package size
     * @param packageUnits the package units
     * @return the relationship, wrapped in a {@link ProductSupplier}, or
     *         <tt>null</tt> if none is found
     */
    public ProductSupplier getProductSupplier(Party supplier, Product product,
                                              int packageSize,
                                              String packageUnits) {
        for (ProductSupplier ps : getProductSuppliers(supplier, product)) {
            if (ps.getPackageSize() == packageSize
                    && ObjectUtils.equals(ps.getPackageUnits(),
                                          packageUnits)) {
                return ps;
            } else if (ps.getPackageSize() == 0) {
                return ps;
            }
        }
        return null;
    }

    /**
     * Returns all active <em>entityRelationship.productSupplier</em>
     * relationships for a particular product.
     *
     * @param product the product
     * @return the relationships, wrapped in {@link ProductSupplier} instances
     */
    public List<ProductSupplier> getProductSuppliers(Product product) {
        List<ProductSupplier> result = new ArrayList<ProductSupplier>();
        EntityBean bean = new EntityBean(product, service);
        List<EntityRelationship> relationships
                = bean.getNodeRelationships("suppliers",
                                            IsActiveRelationship.ACTIVE_NOW);
        for (EntityRelationship relationship : relationships) {
            result.add(new ProductSupplier(relationship, service));
        }
        return result;
    }

    /**
     * Creates a new <em>entityRelationship.productSupplier</em> relationship
     * between a supplier and product.
     *
     * @param product  the product
     * @param supplier the supplier
     * @return the relationship, wrapped in a {@link ProductSupplier}
     */
    public ProductSupplier createProductSupplier(Product product,
                                                 Party supplier) {

        EntityBean bean = new EntityBean(product, service);
        EntityRelationship rel = bean.addRelationship(
                "entityRelationship.productSupplier", supplier);
        return new ProductSupplier(rel, service);
    }

}
