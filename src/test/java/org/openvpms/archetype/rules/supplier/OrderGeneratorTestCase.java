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

import org.junit.Before;
import org.junit.Test;
import org.openvpms.archetype.rules.finance.tax.TaxRules;
import org.openvpms.archetype.rules.practice.PracticeArchetypes;
import org.openvpms.archetype.rules.product.ProductSupplier;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.FinancialAct;
import org.openvpms.component.business.domain.im.lookup.Lookup;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.business.service.archetype.helper.TypeHelper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Tests the {@link OrderGenerator}.
 *
 * @author Tim Anderson
 */
public class OrderGeneratorTestCase extends AbstractSupplierTest {

    /**
     * The tax rules.
     */
    private TaxRules taxRules;

    /**
     * GST tax rate.
     */
    private Lookup gst;


    /**
     * Sets up the test case.
     */
    @Before
    public void setUp() {
        super.setUp();
        Party practice = (Party) create(PracticeArchetypes.PRACTICE);
        gst = TestHelper.createTaxType(BigDecimal.TEN);
        practice.addClassification(gst);
        taxRules = new TaxRules(practice);
    }

    /**
     * Tests the {@link OrderGenerator#getOrderableStock(Party, Party, boolean)}  method.
     */
    @Test
    public void testGetOrderableStock() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();
        Product product2 = TestHelper.createProduct();
        Product product3 = TestHelper.createProduct();
        Product product4 = TestHelper.createProduct();
        Product product5 = TestHelper.createProduct();
        Product product6 = TestHelper.createProduct();

        addRelationships(product1, stockLocation, supplier, true, 1, 10, 5);  // below critical level
        addRelationships(product2, stockLocation, supplier, true, 5, 10, 5);  // at critical level
        addRelationships(product3, stockLocation, supplier, true, 6, 10, 5);  // above critical level
        addRelationships(product4, stockLocation, supplier, true, 9, 10, 5);  // below ideal level
        addRelationships(product5, stockLocation, supplier, true, 10, 10, 5); // at ideal level
        addRelationships(product6, stockLocation, supplier, true, 11, 10, 5); // above ideal level

        // check stock at or below critical levels
        List<Stock> atOrBelowCritical = generator.getOrderableStock(supplier, stockLocation, false);
        assertEquals(2, atOrBelowCritical.size());
        checkStock(atOrBelowCritical, product1, supplier, stockLocation, 1, 0, 9);
        checkStock(atOrBelowCritical, product2, supplier, stockLocation, 5, 0, 5);

        // check stock at or below ideal levels
        List<Stock> atOrBelowIdeal = generator.getOrderableStock(supplier, stockLocation, true);
        assertEquals(4, atOrBelowIdeal.size());
        checkStock(atOrBelowIdeal, product1, supplier, stockLocation, 1, 0, 9);
        checkStock(atOrBelowIdeal, product2, supplier, stockLocation, 5, 0, 5);
        checkStock(atOrBelowIdeal, product3, supplier, stockLocation, 6, 0, 4);
        checkStock(atOrBelowIdeal, product4, supplier, stockLocation, 9, 0, 1);
    }

    /**
     * Checks that the on-hand, on-order and to-order quantities is calculated correctly when there are outstanding
     * orders.
     */
    @Test
    public void testGetOrderableStockForPendingOrders() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier1 = TestHelper.createSupplier();
        Party supplier2 = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();
        Product product2 = TestHelper.createProduct();
        Product product3 = TestHelper.createProduct();

        addRelationships(product1, stockLocation, supplier1, true, 1, 10, 6);
        addRelationships(product2, stockLocation, supplier1, true, 2, 10, 5);
        addRelationships(product3, stockLocation, supplier2, true, 1, 10, 5);
        createOrder(product1, supplier1, stockLocation, 2, OrderStatus.IN_PROGRESS);
        createOrder(product1, supplier1, stockLocation, 1, OrderStatus.COMPLETED);
        createOrder(product1, supplier1, stockLocation, 1, OrderStatus.POSTED);
        createOrder(product1, supplier1, stockLocation, 1, OrderStatus.ACCEPTED);

        createOrder(product2, supplier1, stockLocation, 3, OrderStatus.ACCEPTED);

        // shouldn't impact totals
        createOrder(product1, supplier1, stockLocation, 1, OrderStatus.CANCELLED);
        createOrder(product2, supplier1, stockLocation, 1, OrderStatus.CANCELLED);
        createOrder(product3, supplier2, stockLocation, 1, OrderStatus.IN_PROGRESS);
        createOrder(product3, supplier2, stockLocation, 1, OrderStatus.COMPLETED);
        createOrder(product3, supplier2, stockLocation, 1, OrderStatus.POSTED);
        createOrder(product3, supplier2, stockLocation, 1, OrderStatus.ACCEPTED);

        supplier1 = get(supplier1);
        List<Stock> stock = generator.getOrderableStock(supplier1, stockLocation, false);
        assertEquals(2, stock.size());
        checkStock(stock, product1, supplier1, stockLocation, 1, 5, 4);
        checkStock(stock, product2, supplier1, stockLocation, 2, 3, 5);
    }

    /**
     * Tests creation of an order based on the amount of stock on hand.
     */
    @Test
    public void testCreateOrder() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();
        Product product2 = TestHelper.createProduct();

        addRelationships(product1, stockLocation, supplier, true, 1, 10, 5, new BigDecimal("2.0"), 1);
        addRelationships(product2, stockLocation, supplier, true, 1, 10, 5, BigDecimal.ONE, 1);

        List<FinancialAct> order = generator.createOrder(supplier, stockLocation, false);
        assertEquals(3, order.size());
        FinancialAct act = order.get(0);
        FinancialAct item1 = order.get(1);
        FinancialAct item2 = order.get(2);
        checkEquals(new BigDecimal("29.70"), act.getTotal());
        checkEquals(new BigDecimal("2.70"), act.getTaxAmount());
        assertTrue(TypeHelper.isA(act, SupplierArchetypes.ORDER));
        assertTrue(TypeHelper.isA(item1, SupplierArchetypes.ORDER_ITEM));
        assertTrue(TypeHelper.isA(item2, SupplierArchetypes.ORDER_ITEM));
        save(order);
    }

    /**
     * Verifies that when the idealQty is less the than the package size, an order for a single package will be
     * created.
     */
    @Test
    public void testCreateOrderForQuantityLessThanPackageSize() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();

        addRelationships(product1, stockLocation, supplier, true, 0, 2, 2, new BigDecimal("2.0"), 10);

        List<FinancialAct> order = generator.createOrder(supplier, stockLocation, false);
        assertEquals(2, order.size());
        FinancialAct act = order.get(0);
        FinancialAct item1 = order.get(1);
        checkEquals(new BigDecimal("2.20"), act.getTotal());
        checkEquals(new BigDecimal("0.20"), act.getTaxAmount());
        checkEquals(BigDecimal.ONE, item1.getQuantity());
        assertTrue(TypeHelper.isA(act, SupplierArchetypes.ORDER));
        assertTrue(TypeHelper.isA(item1, SupplierArchetypes.ORDER_ITEM));
        save(order);

    }

    /**
     * Verifies that when the idealQty is greater the than the package size, the order will round up to the correct
     * quantity.
     */
    @Test
    public void testCreateOrderForQuantityGreaterThanPackageSize() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();

        addRelationships(product1, stockLocation, supplier, true, 0, 22, 22, new BigDecimal("2.0"), 10);

        List<FinancialAct> order = generator.createOrder(supplier, stockLocation, false);
        assertEquals(2, order.size());
        FinancialAct act = order.get(0);
        FinancialAct item1 = order.get(1);
        checkEquals(new BigDecimal("6.60"), act.getTotal());
        checkEquals(new BigDecimal("0.60"), act.getTaxAmount());
        checkEquals(BigDecimal.valueOf(3), item1.getQuantity());
        assertTrue(TypeHelper.isA(act, SupplierArchetypes.ORDER));
        assertTrue(TypeHelper.isA(item1, SupplierArchetypes.ORDER_ITEM));
        save(order);
    }

    /**
     * Verifies that tax amounts are rounded correctly, if a product is taxed.
     */
    @Test
    public void testCreateOrderForProductWithTax() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier = TestHelper.createSupplier();
        Product product = TestHelper.createProduct();
        product.addClassification(gst);
        save(product);

        BigDecimal total = new BigDecimal("1292.54");
        BigDecimal taxAmount = new BigDecimal("117.50");
        BigDecimal quantity = new BigDecimal("96");
        addRelationships(product, stockLocation, supplier, true, 0, quantity.intValue(), 0, new BigDecimal("12.24"), 1);

        List<FinancialAct> order = generator.createOrder(supplier, stockLocation, false);
        assertEquals(2, order.size());
        FinancialAct act = order.get(0);
        FinancialAct item1 = order.get(1);
        checkEquals(total, act.getTotal());
        checkEquals(taxAmount, act.getTaxAmount());
        checkEquals(quantity, item1.getQuantity());
        checkEquals(total, item1.getTotal());
        checkEquals(taxAmount, item1.getTaxAmount());
        checkEquals(quantity, item1.getQuantity());
        assertTrue(TypeHelper.isA(act, SupplierArchetypes.ORDER));
        assertTrue(TypeHelper.isA(item1, SupplierArchetypes.ORDER_ITEM));
        save(order);
    }

    /**
     * Verifies that products are ordered if there is no stock, and there are completed orders.
     */
    @Test
    public void testCreateOrderWithPriorDeliveries() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier = TestHelper.createSupplier();
        Product product = TestHelper.createProduct();
        save(product);

        // create a cancelled order of 200 units
        createOrder(product, supplier, stockLocation, 200, OrderStatus.CANCELLED, 0, 0, DeliveryStatus.PENDING);

        // create a fully delivered order of 100 units
        createOrder(product, supplier, stockLocation, 100, OrderStatus.ACCEPTED, 100, 0, DeliveryStatus.FULL);

        // create a part delivered order. 50 units left to deliver
        createOrder(product, supplier, stockLocation, 100, OrderStatus.ACCEPTED, 50, 0, DeliveryStatus.PART);

        // 0 units on hand, want 100
        addRelationships(product, stockLocation, supplier, true, 0, 100, 100, new BigDecimal("2.0"), 1);

        BigDecimal total = new BigDecimal("110.0");
        BigDecimal taxAmount = new BigDecimal("10.0");
        BigDecimal quantity = new BigDecimal("50");

        List<FinancialAct> order = generator.createOrder(supplier, stockLocation, true);
        assertEquals(2, order.size());
        FinancialAct act = order.get(0);
        FinancialAct item1 = order.get(1);
        checkEquals(total, act.getTotal());
        checkEquals(taxAmount, act.getTaxAmount());
        checkEquals(quantity, item1.getQuantity());
        checkEquals(total, item1.getTotal());
        checkEquals(taxAmount, item1.getTaxAmount());
        checkEquals(quantity, item1.getQuantity());
        assertTrue(TypeHelper.isA(act, SupplierArchetypes.ORDER));
        assertTrue(TypeHelper.isA(item1, SupplierArchetypes.ORDER_ITEM));
        save(order);
    }

    /**
     * Verifies that the supplier on an product-stock location relationship is selected over the preferred supplier.
     */
    @Test
    public void testStockLocationSupplier() {
        OrderGenerator generator = new OrderGenerator(taxRules, getArchetypeService());
        Party stockLocation = SupplierTestHelper.createStockLocation();
        Party supplier1 = TestHelper.createSupplier();
        Party supplier2 = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();

        addProductSupplierRelationship(product1, supplier1, true, new BigDecimal("2.0"), 1);
        addProductSupplierRelationship(product1, supplier2, false, new BigDecimal("3.0"), 1);
        addProductStockLocationRelationship(product1, stockLocation, supplier2, 1, 10, 5);
        save(product1, supplier1, supplier2, stockLocation);

        // supplier1 has preferred flag set, but should be ignored as the product-stock location specifies supplier
        List<FinancialAct> order1 = generator.createOrder(supplier1, stockLocation, false);
        assertEquals(0, order1.size());

        // verify order created for supplier2
        List<FinancialAct> order2 = generator.createOrder(supplier2, stockLocation, false);
        assertEquals(2, order2.size());
        FinancialAct act = order2.get(0);
        FinancialAct item1 = order2.get(1);
        ActBean bean = new ActBean(act);
        checkEquals(new BigDecimal("29.70"), act.getTotal());
        checkEquals(new BigDecimal("2.70"), act.getTaxAmount());
        assertTrue(bean.isA(SupplierArchetypes.ORDER));
        assertEquals(supplier2.getObjectReference(), bean.getNodeParticipantRef("supplier"));
        assertTrue(TypeHelper.isA(item1, SupplierArchetypes.ORDER_ITEM));
        save(order2);
    }

    /**
     * Verifies the values in a {@code Stock} match that expected.
     *
     * @param stock         the stock to check
     * @param product       the expected product
     * @param supplier      the expected supplier
     * @param stockLocation the expected stock location
     * @param quantity      the expected on-hand quantity
     * @param onOrder       the expected on-order quantity
     * @param toOrder       the expected to-order quantity
     */
    private void checkStock(List<Stock> stock, Product product, Party supplier, Party stockLocation, int quantity,
                            int onOrder, int toOrder) {
        Stock s = getStock(stock, product);
        assertNotNull(s);
        checkStock(s, product, supplier, stockLocation, quantity, onOrder, toOrder);
    }

    /**
     * Returns a {@link Stock} corresponding to the supplied product.
     *
     * @param stock   the stock list
     * @param product the product
     * @return the corresponding stock, or {@code null} if none is found
     */
    private Stock getStock(List<Stock> stock, Product product) {
        Stock result = null;
        for (Stock s : stock) {
            if (s.getProduct().equals(product)) {
                assertNull(result);
                result = s;
            }
        }
        return result;
    }

    /**
     * Verifies the values in a {@code Stock} match that expected.
     *
     * @param stock         the stock to check
     * @param product       the expected product
     * @param supplier      the expected supplier
     * @param stockLocation the expected stock location
     * @param quantity      the expected on-hand quantity
     * @param onOrder       the expected on-order quantity
     * @param toOrder       the expected to-order quantity
     */
    private void checkStock(Stock stock, Product product, Party supplier, Party stockLocation, int quantity,
                            int onOrder, int toOrder) {
        assertEquals(product, stock.getProduct());
        assertEquals(supplier, stock.getSupplier());
        assertEquals(stockLocation, stock.getStockLocation());
        checkEquals(BigDecimal.valueOf(quantity), stock.getQuantity());
        checkEquals(BigDecimal.valueOf(onOrder), stock.getOnOrder());
        checkEquals(BigDecimal.valueOf(toOrder), stock.getToOrder());
    }

    /**
     * Creates an order.
     *
     * @param product       the product to order
     * @param supplier      the supplier to order from
     * @param stockLocation the stock location for delivery to
     * @param quantity      the order quantity
     * @param status        the order status
     * @return a new order
     */
    private FinancialAct createOrder(Product product, Party supplier, Party stockLocation, int quantity,
                                     String status) {
        return createOrder(product, supplier, stockLocation, quantity, status, 0, 0, DeliveryStatus.PENDING);
    }

    /**
     * Creates an order.
     *
     * @param product       the product to order
     * @param supplier      the supplier to order from
     * @param stockLocation the stock location for delivery to
     * @param quantity      the order quantity
     * @param status        the order status
     * @return a new order
     */
    private FinancialAct createOrder(Product product, Party supplier, Party stockLocation, int quantity,
                                     String status, int receivedQuantity, int cancelledQuantity,
                                     DeliveryStatus deliveryStatus) {
        FinancialAct orderItem = createOrderItem(product, BigDecimal.valueOf(quantity), 1, BigDecimal.ONE);
        ActBean itemBean = new ActBean(orderItem);
        itemBean.setValue("receivedQuantity", BigDecimal.valueOf(receivedQuantity));
        itemBean.setValue("cancelledQuantity", BigDecimal.valueOf(cancelledQuantity));
        FinancialAct order = createOrder(supplier, stockLocation, orderItem);
        ActBean orderBean = new ActBean(order);
        order.setStatus(status);
        orderBean.setValue("deliveryStatus", deliveryStatus.toString());

        save(order, orderItem);
        return order;
    }

    /**
     * Creates relationships between a product and stock location and product and supplier.
     *
     * @param product       the product
     * @param stockLocation the stock location
     * @param supplier      the supplier
     * @param preferred     indicates if the supplier is the preferred supplier
     * @param quantity      the quantity
     * @param idealQty      the ideal quantity
     * @param criticalQty   the critical quantity
     */
    private void addRelationships(Product product, Party stockLocation, Party supplier, boolean preferred,
                                  int quantity, int idealQty, int criticalQty) {
        addRelationships(product, stockLocation, supplier, preferred, quantity, idealQty, criticalQty, BigDecimal.ONE,
                         1);
    }

    /**
     * Creates relationships between a product and stock location and product and supplier.
     *
     * @param product       the product
     * @param stockLocation the stock location
     * @param supplier      the supplier
     * @param preferred     indicates if the supplier is the preferred supplier
     * @param quantity      the quantity
     * @param idealQty      the ideal quantity
     * @param criticalQty   the critical quantity
     * @param unitPrice     the unit price
     */
    private void addRelationships(Product product, Party stockLocation, Party supplier, boolean preferred,
                                  int quantity, int idealQty, int criticalQty, BigDecimal unitPrice, int packageSize) {
        addProductStockLocationRelationship(product, stockLocation, null, quantity, idealQty, criticalQty);
        addProductSupplierRelationship(product, supplier, preferred, unitPrice, packageSize);
    }

    private void addProductSupplierRelationship(Product product, Party supplier, boolean preferred, BigDecimal unitPrice,
                                                int packageSize) {
        EntityBean bean = new EntityBean(product);
        ProductSupplier ps = new ProductSupplier(bean.addNodeRelationship("suppliers", supplier));
        ps.setPreferred(preferred);
        ps.setPackageSize(packageSize);
        ps.setNettPrice(unitPrice);
        save(product, supplier);
    }

    private void addProductStockLocationRelationship(Product product, Party stockLocation, Party supplier,
                                                     int quantity, int idealQty, int criticalQty) {
        EntityBean bean = new EntityBean(product);
        IMObjectBean productStockLocation = new IMObjectBean(bean.addNodeRelationship("stockLocations", stockLocation));
        if (supplier != null) {
            productStockLocation.setValue("supplier", supplier.getObjectReference());
        }
        productStockLocation.setValue("quantity", quantity);
        productStockLocation.setValue("idealQty", idealQty);
        productStockLocation.setValue("criticalQty", criticalQty);
        save(product, stockLocation);
    }
}
