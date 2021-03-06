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
 * Copyright 2015 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.rules.supplier;

import org.junit.Test;
import org.openvpms.archetype.rules.act.ActStatus;
import org.openvpms.archetype.rules.finance.tax.TaxRules;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.act.FinancialAct;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.business.service.archetype.helper.TypeHelper;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Tests the {@link OrderRules} class.
 *
 * @author Tim Anderson
 */
public class OrderRulesTestCase extends AbstractSupplierTest {

    /**
     * The rules.
     */
    private OrderRules rules;


    /**
     * Tests the {@link OrderRules#getDeliveryStatus(FinancialAct)} method.
     */
    @Test
    public void testGetDeliveryStatus() {
        BigDecimal two = new BigDecimal("2.0");
        BigDecimal three = new BigDecimal("3.0");

        FinancialAct act = (FinancialAct) create(SupplierArchetypes.ORDER_ITEM);
        assertEquals(DeliveryStatus.PENDING, rules.getDeliveryStatus(act));

        checkDeliveryStatus(act, three, ZERO, ZERO, DeliveryStatus.PENDING);
        checkDeliveryStatus(act, three, three, ZERO, DeliveryStatus.FULL);
        checkDeliveryStatus(act, three, two, ZERO, DeliveryStatus.PART);
        checkDeliveryStatus(act, three, two, ONE, DeliveryStatus.FULL);
        checkDeliveryStatus(act, three, ZERO, three, DeliveryStatus.FULL);
    }

    /**
     * Tests the {@link OrderRules#copyOrder(FinancialAct, String)} method.
     */
    @Test
    public void testCopyOrder() {
        BigDecimal quantity = new BigDecimal(50);
        int packageSize = 10;
        BigDecimal unitPrice = BigDecimal.ONE;
        FinancialAct orderItem = createOrderItem(quantity, packageSize, unitPrice);
        IMObjectBean itemBean = new IMObjectBean(orderItem);
        itemBean.setValue("receivedQuantity", 25);
        itemBean.setValue("cancelledQuantity", 10);
        FinancialAct order = createOrder(orderItem);
        IMObjectBean orderBean = new IMObjectBean(order);
        orderBean.setValue("status", ActStatus.POSTED);
        orderBean.setValue("deliveryStatus", DeliveryStatus.PART.toString());
        orderBean.save();

        FinancialAct copy = rules.copyOrder(order, "Copy");
        assertTrue(TypeHelper.isA(copy, SupplierArchetypes.ORDER));
        assertEquals(ActStatus.IN_PROGRESS, copy.getStatus());
        assertFalse(copy.equals(order));

        ActBean bean = new ActBean(copy);
        List<Act> items = bean.getNodeActs("items");
        assertEquals(1, items.size());

        FinancialAct copyItem = (FinancialAct) items.get(0);
        assertTrue(TypeHelper.isA(copyItem, SupplierArchetypes.ORDER_ITEM));
        assertFalse(copyItem.equals(orderItem));
        checkEquals(quantity, copyItem.getQuantity());
        IMObjectBean copyBean = new IMObjectBean(copyItem);
        assertEquals(0, copyBean.getInt("receivedQuantity"));
        assertEquals(0, copyBean.getInt("cancelledQuantity"));

        assertEquals(DeliveryStatus.PENDING.toString(), bean.getString("deliveryStatus"));
        assertEquals("Copy", copy.getTitle());
    }

    /**
     * Tests the {@link OrderRules#createDeliveryItem(FinancialAct)} method.
     */
    @Test
    public void testCreateDeliveryItem() {
        BigDecimal quantity = new BigDecimal(50);
        BigDecimal received = new BigDecimal(40);
        BigDecimal cancelled = new BigDecimal(4);
        BigDecimal expectedQuantity = new BigDecimal(6);

        int packageSize = 10;
        BigDecimal unitPrice = BigDecimal.ONE;

        FinancialAct orderItem = createOrderItem(quantity, packageSize,
                                                 unitPrice);
        ActBean itemBean = new ActBean(orderItem);
        itemBean.setValue("receivedQuantity", received);
        itemBean.setValue("cancelledQuantity", cancelled);
        FinancialAct order = createOrder(orderItem);
        order.setStatus(ActStatus.POSTED);
        save(order);

        FinancialAct item = rules.createDeliveryItem(orderItem);

        assertTrue(TypeHelper.isA(item, SupplierArchetypes.DELIVERY_ITEM));
        checkEquals(expectedQuantity, item.getQuantity());

        // the delivery item shouldn't have any relationships
        ActBean bean = new ActBean(item);
        assertTrue(bean.getActs().isEmpty());
    }

    /**
     * Tests the {@link OrderRules#createReturnItem} method.
     */
    @Test
    public void testCreateReturnItem() {
        BigDecimal quantity = new BigDecimal(50);
        BigDecimal received = new BigDecimal(40);
        BigDecimal cancelled = new BigDecimal(4);

        int packageSize = 10;
        BigDecimal unitPrice = BigDecimal.ONE;

        FinancialAct orderItem = createOrderItem(quantity, packageSize, unitPrice);
        ActBean itemBean = new ActBean(orderItem);
        itemBean.setValue("receivedQuantity", received);
        itemBean.setValue("cancelledQuantity", cancelled);
        FinancialAct order = createOrder(orderItem);
        order.setStatus(ActStatus.POSTED);
        save(order);

        FinancialAct item = rules.createReturnItem(orderItem);

        assertTrue(TypeHelper.isA(item, SupplierArchetypes.RETURN_ITEM));
        checkEquals(received, item.getQuantity());

        // the return item shouldn't have any relationships
        ActBean bean = new ActBean(item);
        assertTrue(bean.getActs().isEmpty());
    }

    /**
     * Tests the {@link OrderRules#invoiceSupplier(Act)} method.
     */
    @Test
    public void testInvoiceSupplier() {
        BigDecimal quantity = new BigDecimal(50);
        int packageSize = 10;
        BigDecimal unitPrice = BigDecimal.ONE;
        BigDecimal total = quantity.multiply(unitPrice);

        FinancialAct orderItem = createOrderItem(quantity, packageSize,
                                                 unitPrice);

        FinancialAct delivery = createDelivery(quantity, packageSize, unitPrice,
                                               orderItem);
        delivery.setStatus(ActStatus.POSTED);
        save(delivery);

        FinancialAct invoice = rules.invoiceSupplier(delivery);
        assertTrue(TypeHelper.isA(invoice, SupplierArchetypes.INVOICE));
        assertEquals(ActStatus.IN_PROGRESS, invoice.getStatus());

        ActBean bean = new ActBean(invoice);
        List<Act> acts = bean.getActs();
        assertEquals(1, acts.size());

        FinancialAct item = (FinancialAct) acts.get(0);
        assertTrue(TypeHelper.isA(item, SupplierArchetypes.INVOICE_ITEM));
        checkEquals(total, item.getTotal());

        // verify there is a relationship from the delivery to the invoice
        ActBean deliveryBean = new ActBean(delivery);
        List<Act> invoices = deliveryBean.getNodeActs("invoice");
        assertEquals(1, invoices.size());
        assertEquals(invoice, invoices.get(0));

        try {
            rules.invoiceSupplier(delivery);
            fail("Expected invoicing to fail");
        } catch (IllegalStateException exception) {
            // the expected
        }
    }

    /**
     * Tests the {@link OrderRules#creditSupplier(Act)} method.
     */
    @Test
    public void testCreditSupplier() {
        BigDecimal quantity = new BigDecimal(50);
        int packageSize = 10;
        BigDecimal unitPrice = BigDecimal.ONE;

        FinancialAct orderItem = createOrderItem(quantity, packageSize,
                                                 unitPrice);
        Act orderReturn = createReturn(quantity, packageSize, unitPrice,
                                       orderItem);
        orderReturn.setStatus(ActStatus.POSTED);
        save(orderReturn);

        FinancialAct credit = rules.creditSupplier(orderReturn);
        assertTrue(TypeHelper.isA(credit, SupplierArchetypes.CREDIT));
        assertEquals(ActStatus.IN_PROGRESS, credit.getStatus());

        ActBean bean = new ActBean(credit);
        List<Act> acts = bean.getActs();
        assertEquals(1, acts.size());

        Act item = acts.get(0);
        assertTrue(TypeHelper.isA(item, SupplierArchetypes.CREDIT_ITEM));

        // verify there is a relationship from the return to the credit
        ActBean returnBean = new ActBean(orderReturn);
        List<Act> credits = returnBean.getNodeActs("returnCredit");
        assertEquals(1, credits.size());
        assertEquals(credit, credits.get(0));

        try {
            rules.creditSupplier(orderReturn);
            fail("Expected crediting to fail");
        } catch (IllegalStateException exception) {
            // the expected
        }
    }

    /**
     * Tests the {@link OrderRules#reverseDelivery(Act)} method.
     */
    @Test
    public void testReverseDelivery() {
        BigDecimal quantity = new BigDecimal(50);
        BigDecimal unitPrice = BigDecimal.ONE;
        int packageSize = 10;

        FinancialAct orderItem = createOrderItem(quantity, packageSize,
                                                 unitPrice);
        Act delivery = createDelivery(quantity, packageSize, unitPrice,
                                      orderItem);
        delivery.setStatus(ActStatus.POSTED);
        save(delivery);
        rules.invoiceSupplier(delivery); // create an invoice from the delivery

        Act reversal = rules.reverseDelivery(delivery);
        checkReversal(reversal, SupplierArchetypes.RETURN,
                      SupplierArchetypes.RETURN_ITEM, orderItem);

        // verify the invoice hasn't been reversed. Probably should be. TODO
        ActBean bean = new ActBean(reversal);
        assertTrue(bean.getNodeActs("returnCredit").isEmpty());
    }

    /**
     * Tests the {@link OrderRules#reverseReturn(Act)} method.
     */
    @Test
    public void testReverseReturn() {
        BigDecimal quantity = new BigDecimal(50);
        int packageSize = 10;
        BigDecimal unitPrice = BigDecimal.ONE;

        FinancialAct orderItem = createOrderItem(quantity, packageSize,
                                                 unitPrice);
        Act orderReturn = createReturn(quantity, packageSize, unitPrice,
                                       orderItem);
        orderReturn.setStatus(ActStatus.POSTED);
        save(orderReturn);

        // create a credit for the return
        rules.creditSupplier(orderReturn);
        ActBean returnBean = new ActBean(orderReturn);
        assertEquals(1, returnBean.getNodeActs("returnCredit").size());

        Act reversal = rules.reverseReturn(orderReturn);
        checkReversal(reversal, SupplierArchetypes.DELIVERY,
                      SupplierArchetypes.DELIVERY_ITEM, orderItem);

        // verify the credit hasn't been reversed. Probably should be. TODO
        ActBean bean = new ActBean(reversal);
        assertTrue(bean.getNodeActs("invoice").isEmpty());
    }

    /**
     * Sets up the test case.
     */
    @Override
    public void setUp() {
        super.setUp();
        TaxRules taxRules = new TaxRules(TestHelper.getPractice(), getArchetypeService(), getLookupService());
        rules = new OrderRules(taxRules, getArchetypeService());
    }

    /**
     * Verifies the delivery status matches that expected for the supplied values.
     *
     * @param act       the act
     * @param quantity  the quantity
     * @param received  the received quantity
     * @param cancelled the cancelled quantity
     * @param expected  the expected delivery status
     */
    private void checkDeliveryStatus(FinancialAct act, BigDecimal quantity, BigDecimal received, BigDecimal cancelled,
                                     DeliveryStatus expected) {
        ActBean bean = new ActBean(act);
        bean.setValue("quantity", quantity);
        bean.setValue("receivedQuantity", received);
        bean.setValue("cancelledQuantity", cancelled);
        assertEquals(expected, rules.getDeliveryStatus(act));
    }

    /**
     * Verifies a reversal matches that expected.
     *
     * @param reversal      the reversal
     * @param shortName     the expected archetype short name
     * @param itemShortName the expected item short name
     * @param orderItem     the expected order item
     */
    private void checkReversal(Act reversal, String shortName,
                               String itemShortName, FinancialAct orderItem) {
        assertTrue(TypeHelper.isA(reversal, shortName));
        assertEquals(ActStatus.IN_PROGRESS, reversal.getStatus());

        ActBean bean = new ActBean(reversal);
        List<Act> items = bean.getNodeActs("items");
        assertEquals(1, items.size());

        Act item = items.get(0);
        assertTrue(TypeHelper.isA(item, itemShortName));

        ActBean itemBean = new ActBean(item);
        List<Act> orders = itemBean.getNodeActs("order");
        assertEquals(1, orders.size());
        assertEquals(orderItem, orders.get(0));
    }

}
