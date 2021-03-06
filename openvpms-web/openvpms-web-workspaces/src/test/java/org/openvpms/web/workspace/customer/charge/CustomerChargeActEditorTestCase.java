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

package org.openvpms.web.workspace.customer.charge;

import org.junit.Before;
import org.junit.Test;
import org.openvpms.archetype.rules.act.ActCalculator;
import org.openvpms.archetype.rules.act.ActStatus;
import org.openvpms.archetype.rules.finance.account.CustomerAccountArchetypes;
import org.openvpms.archetype.rules.finance.account.CustomerAccountRules;
import org.openvpms.archetype.rules.finance.account.FinancialTestHelper;
import org.openvpms.archetype.rules.finance.discount.DiscountRules;
import org.openvpms.archetype.rules.finance.discount.DiscountTestHelper;
import org.openvpms.archetype.rules.patient.InvestigationActStatus;
import org.openvpms.archetype.rules.patient.MedicalRecordRules;
import org.openvpms.archetype.rules.patient.prescription.PrescriptionTestHelper;
import org.openvpms.archetype.rules.patient.reminder.ReminderStatus;
import org.openvpms.archetype.rules.product.ProductArchetypes;
import org.openvpms.archetype.rules.product.ProductTestHelper;
import org.openvpms.archetype.rules.stock.StockArchetypes;
import org.openvpms.archetype.rules.stock.StockRules;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.act.FinancialAct;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.datatypes.quantity.Money;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.domain.im.security.User;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.TypeHelper;
import org.openvpms.web.component.app.LocalContext;
import org.openvpms.web.component.im.edit.SaveHelper;
import org.openvpms.web.component.im.layout.DefaultLayoutContext;
import org.openvpms.web.component.im.layout.LayoutContext;
import org.openvpms.web.component.property.DefaultValidator;
import org.openvpms.web.component.property.Validator;
import org.openvpms.web.component.property.ValidatorError;
import org.openvpms.web.echo.help.HelpContext;
import org.openvpms.web.resource.i18n.Messages;
import org.openvpms.web.resource.i18n.format.NumberFormatter;
import org.openvpms.web.system.ServiceHelper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openvpms.archetype.rules.product.ProductArchetypes.MEDICATION;
import static org.openvpms.web.workspace.customer.charge.CustomerChargeTestHelper.checkOrder;
import static org.openvpms.web.workspace.customer.charge.CustomerChargeTestHelper.createPharmacy;
import static org.openvpms.web.workspace.customer.charge.TestPharmacyOrderService.Order;

/**
 * Tests the {@link CustomerChargeActEditor} class.
 *
 * @author Tim Anderson
 */
public class CustomerChargeActEditorTestCase extends AbstractCustomerChargeActEditorTest {

    /**
     * The customer.
     */
    private Party customer;

    /**
     * The patient.
     */
    private Party patient;

    /**
     * The author.
     */
    private User author;

    /**
     * The clinician.
     */
    private User clinician;

    /**
     * The practice location.
     */
    private Party location;

    /**
     * The layout context.
     */
    private LayoutContext layoutContext;

    /**
     * Medical record rules.
     */
    private MedicalRecordRules records;


    /**
     * Sets up the test case.
     */
    @Before
    @Override
    public void setUp() {
        super.setUp();
        customer = TestHelper.createCustomer();
        patient = TestHelper.createPatient(customer);
        author = TestHelper.createUser();
        clinician = TestHelper.createClinician();
        location = TestHelper.createLocation();

        layoutContext = new DefaultLayoutContext(new LocalContext(), new HelpContext("foo", null));
        layoutContext.getContext().setPractice(getPractice());
        layoutContext.getContext().setCustomer(customer);
        layoutContext.getContext().setUser(author);
        layoutContext.getContext().setClinician(clinician);
        layoutContext.getContext().setLocation(location);

        records = ServiceHelper.getBean(MedicalRecordRules.class);
    }

    /**
     * Tests creation and saving of an empty invoice.
     */
    @Test
    public void testEmptyInvoice() {
        checkEmptyCharge((FinancialAct) create(CustomerAccountArchetypes.INVOICE));
    }

    /**
     * Tests creation and saving of an empty invoice.
     */
    @Test
    public void testEmptyCredit() {
        checkEmptyCharge((FinancialAct) create(CustomerAccountArchetypes.CREDIT));
    }

    /**
     * Tests creation and saving of an empty counter sale.
     */
    @Test
    public void testEmptyCounterSale() {
        checkEmptyCharge((FinancialAct) create(CustomerAccountArchetypes.COUNTER));
    }

    /**
     * Tests invoicing.
     */
    @Test
    public void testInvoice() {
        checkEditCharge((FinancialAct) create(CustomerAccountArchetypes.INVOICE));
    }

    /**
     * Tests counter sales.
     */
    @Test
    public void testCounterSale() {
        checkEditCharge((FinancialAct) create(CustomerAccountArchetypes.COUNTER));
    }

    /**
     * Tests credits.
     */
    @Test
    public void testCredit() {
        checkEditCharge((FinancialAct) create(CustomerAccountArchetypes.CREDIT));
    }

    /**
     * Tests the addition of 3 items to an invoice.
     */
    @Test
    public void testAdd3Items() {
        BigDecimal itemTotal1 = BigDecimal.valueOf(20);
        BigDecimal itemTotal2 = BigDecimal.valueOf(50);
        BigDecimal itemTotal3 = new BigDecimal("41.25");

        Product product1 = createProduct(ProductArchetypes.SERVICE, itemTotal1);
        Product product2 = createProduct(ProductArchetypes.SERVICE, itemTotal2);
        Product product3 = createProduct(ProductArchetypes.SERVICE, itemTotal3);

        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
        BigDecimal total = itemTotal1.add(itemTotal2).add(itemTotal3);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        BigDecimal quantity = ONE;
        addItem(editor, patient, product1, quantity, queue);
        addItem(editor, patient, product2, quantity, queue);
        addItem(editor, patient, product3, quantity, queue);
        assertTrue(SaveHelper.save(editor));

        checkTotal(charge, total);
    }

    /**
     * Tests the addition of 3 items to an invoice, followed by the deletion of 1, verifying totals.
     */
    @Test
    public void testAdd3ItemsWithDeletion() {
        BigDecimal itemTotal1 = BigDecimal.valueOf(20);
        BigDecimal itemTotal2 = BigDecimal.valueOf(50);
        BigDecimal itemTotal3 = new BigDecimal("41.25");

        Product product1 = createProduct(ProductArchetypes.SERVICE, itemTotal1);
        Product product2 = createProduct(ProductArchetypes.SERVICE, itemTotal2);
        Product product3 = createProduct(ProductArchetypes.SERVICE, itemTotal3);

        for (int i = 0, j = 0; i < 3; ++i) {
            FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
            BigDecimal total = itemTotal1.add(itemTotal2).add(itemTotal3);

            TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
            editor.getComponent();
            assertTrue(editor.isValid());

            BigDecimal quantity = ONE;
            ChargeEditorQueue queue = editor.getQueue();
            CustomerChargeActItemEditor itemEditor1 = addItem(editor, patient, product1, quantity, queue);
            CustomerChargeActItemEditor itemEditor2 = addItem(editor, patient, product2, quantity, queue);
            CustomerChargeActItemEditor itemEditor3 = addItem(editor, patient, product3, quantity, queue);
            assertTrue(SaveHelper.save(editor));

            charge = get(charge);
            assertTrue(charge.getTotal().compareTo(total) == 0);
            ActCalculator calculator = new ActCalculator(getArchetypeService());
            BigDecimal itemTotal = calculator.sum(charge, "total");
            assertTrue(itemTotal.compareTo(total) == 0);

            if (j == 0) {
                editor.delete((Act) itemEditor1.getObject());
                total = total.subtract(itemTotal1);
            } else if (j == 1) {
                editor.delete((Act) itemEditor2.getObject());
                total = total.subtract(itemTotal2);
            } else if (j == 2) {
                editor.delete((Act) itemEditor3.getObject());
                total = total.subtract(itemTotal3);
            }
            ++j;
            if (j > 2) {
                j = 0;
            }
            assertTrue(SaveHelper.save(editor));
            charge = get(charge);
            checkTotal(charge, total);
        }
    }

    /**
     * Tests the addition of 3 items to an invoice, followed by the deletion of 1 in a new editor, verifying totals.
     */
    @Test
    public void testAdd3ItemsWithDeletionAfterReload() {
        BigDecimal itemTotal1 = BigDecimal.valueOf(20);
        BigDecimal itemTotal2 = BigDecimal.valueOf(50);
        BigDecimal itemTotal3 = new BigDecimal("41.25");

        Product product1 = createProduct(ProductArchetypes.SERVICE, itemTotal1);
        Product product2 = createProduct(ProductArchetypes.SERVICE, itemTotal2);
        Product product3 = createProduct(ProductArchetypes.SERVICE, itemTotal3);

        for (int i = 0, j = 0; i < 3; ++i) {
            FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
            BigDecimal total = itemTotal1.add(itemTotal2).add(itemTotal3);

            TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
            ChargeEditorQueue queue = editor.getQueue();
            editor.getComponent();
            assertTrue(editor.isValid());

            BigDecimal quantity = ONE;
            CustomerChargeActItemEditor itemEditor1 = addItem(editor, patient, product1, quantity, queue);
            CustomerChargeActItemEditor itemEditor2 = addItem(editor, patient, product2, quantity, queue);
            CustomerChargeActItemEditor itemEditor3 = addItem(editor, patient, product3, quantity, queue);
            assertTrue(SaveHelper.save(editor));

            charge = get(charge);
            checkTotal(charge, total);

            editor = createCustomerChargeActEditor(charge, layoutContext);
            editor.getComponent();

            if (j == 0) {
                editor.delete((Act) itemEditor1.getObject());
                total = total.subtract(itemTotal1);
            } else if (j == 1) {
                editor.delete((Act) itemEditor2.getObject());
                total = total.subtract(itemTotal2);
            } else if (j == 2) {
                editor.delete((Act) itemEditor3.getObject());
                total = total.subtract(itemTotal3);
            }
            ++j;
            if (j > 2) {
                j = 0;
            }
            assertTrue(SaveHelper.save(editor));
            charge = get(charge);
            checkTotal(charge, total);
        }
    }

    /**
     * Tests the addition of 3 items to an invoice, followed by the deletion of 1 before saving.
     */
    @Test
    public void test3ItemsAdditionWithDeletionBeforeSave() {
        BigDecimal itemTotal1 = BigDecimal.valueOf(20);
        BigDecimal itemTotal2 = BigDecimal.valueOf(50);
        BigDecimal itemTotal3 = new BigDecimal("41.25");

        Product product1 = createProduct(ProductArchetypes.SERVICE, itemTotal1);
        Product product2 = createProduct(ProductArchetypes.SERVICE, itemTotal2);
        Product product3 = createProduct(ProductArchetypes.SERVICE, itemTotal3);

        for (int i = 0, j = 0; i < 3; ++i) {
            FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
            BigDecimal total = itemTotal1.add(itemTotal2).add(itemTotal3);

            TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
            ChargeEditorQueue queue = editor.getQueue();
            editor.getComponent();

            BigDecimal quantity = ONE;
            CustomerChargeActItemEditor itemEditor1 = addItem(editor, patient, product1, quantity, queue);
            CustomerChargeActItemEditor itemEditor2 = addItem(editor, patient, product2, quantity, queue);
            CustomerChargeActItemEditor itemEditor3 = addItem(editor, patient, product3, quantity, queue);

            if (j == 0) {
                editor.delete((Act) itemEditor1.getObject());
                total = total.subtract(itemTotal1);
            } else if (j == 1) {
                editor.delete((Act) itemEditor2.getObject());
                total = total.subtract(itemTotal2);
            } else if (j == 2) {
                editor.delete((Act) itemEditor3.getObject());
                total = total.subtract(itemTotal3);
            }
            ++j;
            if (j > 2) {
                j = 0;
            }

            assertTrue(SaveHelper.save(editor));
            charge = get(charge);
            checkTotal(charge, total);
        }
    }

    /**
     * Tests the addition of 2 items to an invoice, followed by the change of product of 1 before saving.
     */
    @Test
    public void testItemChange() {
        BigDecimal itemTotal1 = BigDecimal.valueOf(20);
        BigDecimal itemTotal2 = BigDecimal.valueOf(50);
        BigDecimal itemTotal3 = new BigDecimal("41.25");

        Product product1 = createProduct(ProductArchetypes.SERVICE, itemTotal1);
        Product product2 = createProduct(ProductArchetypes.SERVICE, itemTotal2);
        Product product3 = createProduct(ProductArchetypes.SERVICE, itemTotal3);

        for (int i = 0, j = 0; i < 3; ++i) {
            FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
            BigDecimal total = itemTotal1.add(itemTotal2).add(itemTotal3);

            boolean addDefaultItem = (j == 0);
            TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext, addDefaultItem);
            editor.getComponent();

            BigDecimal quantity = ONE;
            CustomerChargeActItemEditor itemEditor1;
            if (j == 0) {
                itemEditor1 = editor.getCurrentEditor();
                setItem(editor, itemEditor1, patient, product1, quantity, editor.getQueue());
            } else {
                itemEditor1 = addItem(editor, patient, product1, quantity, editor.getQueue());
            }
            CustomerChargeActItemEditor itemEditor2 = addItem(editor, patient, product2, quantity, editor.getQueue());

            if (j == 0) {
                itemEditor1.setProduct(product3);
                total = total.subtract(itemTotal1);
            } else if (j == 1) {
                itemEditor2.setProduct(product3);
                total = total.subtract(itemTotal2);
            }
            ++j;
            if (j > 1) {
                j = 0;
            }

            assertTrue(SaveHelper.save(editor));
            charge = get(charge);
            checkTotal(charge, total);
        }
    }

    /**
     * Verifies that the {@link CustomerChargeActEditor#delete()} method deletes an invoice and its item.
     * <p/>
     * If any pharmacy orders have been created, these are cancelled.
     */
    @Test
    public void testDeleteInvoice() {
        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);

        BigDecimal fixedPrice = BigDecimal.valueOf(11);
        BigDecimal itemTotal = BigDecimal.valueOf(11);
        BigDecimal total = itemTotal.multiply(BigDecimal.valueOf(3));

        Entity pharmacy = CustomerChargeTestHelper.createPharmacy(location);
        Product product1 = createProduct(MEDICATION, fixedPrice, pharmacy);
        Entity reminderType1 = addReminder(product1);
        Entity investigationType1 = addInvestigation(product1);
        Entity template1 = addTemplate(product1);

        Product product2 = createProduct(ProductArchetypes.MERCHANDISE, fixedPrice, pharmacy);
        Entity reminderType2 = addReminder(product2);
        Entity investigationType2 = addInvestigation(product2);
        Entity investigationType3 = addInvestigation(product2);
        Entity template2 = addTemplate(product2);

        Product product3 = createProduct(ProductArchetypes.SERVICE, fixedPrice);
        Entity reminderType3 = addReminder(product3);
        Entity investigationType4 = addInvestigation(product3);
        Entity investigationType5 = addInvestigation(product3);
        Entity investigationType6 = addInvestigation(product3);

        Entity template3 = addTemplate(product3);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        BigDecimal quantity = ONE;
        CustomerChargeActItemEditor item1Editor = addItem(editor, patient, product1, quantity, queue);
        CustomerChargeActItemEditor item2Editor = addItem(editor, patient, product2, quantity, queue);
        CustomerChargeActItemEditor item3Editor = addItem(editor, patient, product3, quantity, queue);
        FinancialAct item1 = (FinancialAct) item1Editor.getObject();
        FinancialAct item2 = (FinancialAct) item2Editor.getObject();
        FinancialAct item3 = (FinancialAct) item3Editor.getObject();

        assertTrue(SaveHelper.save(editor));
        List<Order> orders = editor.getPharmacyOrderService().getOrders();
        assertEquals(2, orders.size());
        editor.getPharmacyOrderService().clear();

        BigDecimal balance = (charge.isCredit()) ? total.negate() : total;
        checkBalance(customer, balance, ZERO);

        Act investigation1 = getInvestigation(item1, investigationType1);
        Act investigation2 = getInvestigation(item2, investigationType2);
        Act investigation3 = getInvestigation(item2, investigationType3);
        Act investigation4 = getInvestigation(item3, investigationType4);
        Act investigation5 = getInvestigation(item3, investigationType5);
        Act investigation6 = getInvestigation(item3, investigationType6);

        investigation1.setStatus(InvestigationActStatus.IN_PROGRESS);
        investigation2.setStatus(InvestigationActStatus.COMPLETED);
        investigation3.setStatus(InvestigationActStatus.CANCELLED);
        investigation4.setStatus(InvestigationActStatus.PRELIMINARY);
        investigation5.setStatus(InvestigationActStatus.FINAL);
        investigation6.setStatus(InvestigationActStatus.RECEIVED);
        save(investigation1, investigation2, investigation3, investigation4, investigation5, investigation6);
        Act reminder1 = getReminder(item1, reminderType1);
        Act reminder2 = getReminder(item2, reminderType2);
        Act reminder3 = getReminder(item3, reminderType3);
        reminder1.setStatus(ReminderStatus.IN_PROGRESS);
        reminder2.setStatus(ReminderStatus.COMPLETED);
        reminder3.setStatus(ReminderStatus.CANCELLED);
        save(reminder1, reminder2, reminder3);

        Act doc1 = getDocument(item1, template1);
        Act doc2 = getDocument(item2, template2);
        Act doc3 = getDocument(item3, template3);
        doc1.setStatus(ActStatus.IN_PROGRESS);
        doc2.setStatus(ActStatus.COMPLETED);
        doc3.setStatus(ActStatus.POSTED);
        save(doc1, doc2, doc3);

        charge = get(charge);
        ActBean bean = new ActBean(charge);
        List<FinancialAct> items = bean.getNodeActs("items", FinancialAct.class);
        assertEquals(3, items.size());

        assertTrue(delete(editor));
        assertNull(get(charge));
        for (FinancialAct item : items) {
            assertNull(get(item));
        }

        orders = editor.getPharmacyOrderService().getOrders(true);
        assertEquals(2, orders.size());
        checkOrder(orders.get(0), Order.Type.CANCEL, patient, product1, quantity, item1.getId(),
                   item1.getActivityStartTime(), clinician, pharmacy);
        checkOrder(orders.get(1), Order.Type.CANCEL, patient, product2, quantity, item2.getId(),
                   item2.getActivityStartTime(), clinician, pharmacy);

        assertNull(get(investigation1));
        assertNotNull(get(investigation2));
        assertNull(get(investigation3));
        assertNotNull(get(investigation4));
        assertNotNull(get(investigation5));
        assertNotNull(get(investigation6));

        assertNull(get(reminder1));
        assertNotNull(get(reminder2));
        assertNull(get(reminder3));

        assertNull(get(doc1));
        assertNotNull(get(doc2));
        assertNotNull(get(doc3));

        checkBalance(customer, ZERO, ZERO);
    }

    /**
     * Verifies that the {@link CustomerChargeActEditor#delete()} method deletes a credit and its item.
     */
    @Test
    public void testDeleteCredit() {
        checkDeleteCharge((FinancialAct) create(CustomerAccountArchetypes.CREDIT));
    }

    /**
     * Verifies that the {@link CustomerChargeActEditor#delete()} method deletes a counter sale and its item.
     */
    @Test
    public void testDeleteCounterSale() {
        checkDeleteCharge((FinancialAct) create(CustomerAccountArchetypes.COUNTER));
    }

    /**
     * Verifies stock quantities update for products used in an invoice.
     */
    @Test
    public void testInvoiceStockUpdate() {
        checkChargeStockUpdate((FinancialAct) create(CustomerAccountArchetypes.INVOICE));
    }

    /**
     * Verifies stock quantities update for products used in a credit.
     */
    @Test
    public void testCreditStockUpdate() {
        checkChargeStockUpdate((FinancialAct) create(CustomerAccountArchetypes.CREDIT));
    }

    /**
     * Verifies stock quantities update for products used in a counter sale.
     */
    @Test
    public void testCounterSaleStockUpdate() {
        checkChargeStockUpdate((FinancialAct) create(CustomerAccountArchetypes.COUNTER));
    }

    /**
     * Test template expansion for an invoice.
     */
    @Test
    public void testExpandTemplateInvoice() {
        checkExpandTemplate((FinancialAct) create(CustomerAccountArchetypes.INVOICE));
    }

    /**
     * Test template expansion for a credit.
     */
    @Test
    public void testExpandTemplateCredit() {
        checkExpandTemplate((FinancialAct) create(CustomerAccountArchetypes.CREDIT));
    }

    /**
     * Test template expansion for a counter sale.
     */
    @Test
    public void testExpandTemplateCounterSale() {
        checkExpandTemplate((FinancialAct) create(CustomerAccountArchetypes.COUNTER));
    }

    /**
     * Verifies that an act is invalid if the sum of the item totals don't add up to the charge total.
     */
    @Test
    public void testTotalMismatch() {
        BigDecimal itemTotal = BigDecimal.valueOf(20);
        Product product1 = createProduct(ProductArchetypes.SERVICE, itemTotal);

        List<FinancialAct> acts = FinancialTestHelper.createChargesInvoice(itemTotal, customer, patient, product1,
                                                                           ActStatus.IN_PROGRESS);
        save(acts);
        FinancialAct charge = acts.get(0);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        editor.getComponent();
        charge.setTotal(Money.ONE);
        assertFalse(editor.isValid());

        Validator validator = new DefaultValidator();
        assertFalse(editor.validate(validator));
        List<ValidatorError> list = validator.getErrors(editor);
        assertEquals(1, list.size());
        String message = Messages.format("act.validation.totalMismatch", editor.getProperty("amount").getDisplayName(),
                                         NumberFormatter.formatCurrency(charge.getTotal()),
                                         editor.getProperty("items").getDisplayName(),
                                         NumberFormatter.formatCurrency(itemTotal));
        String expected = Messages.format(ValidatorError.MSG_KEY, message);
        assertEquals(expected, list.get(0).toString());
    }

    /**
     * Verifies a prescription can be selected during invoicing.
     */
    @Test
    public void testPrescription() {
        Product product1 = createProduct(MEDICATION, ONE);

        Act prescription = PrescriptionTestHelper.createPrescription(patient, product1, clinician);
        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        CustomerChargeActItemEditor itemEditor = addItem(editor, patient, product1, ONE, queue);
        assertTrue(SaveHelper.save(editor));

        checkPrescription(prescription, itemEditor);
    }

    /**
     * Verifies that an invoice item linked to a prescription can be deleted, and that the medication is removed
     * from the prescription.
     */
    @Test
    public void testDeleteInvoiceItemLinkedToPrescription() {
        Product product1 = createProduct(MEDICATION, ONE);
        Product product2 = createProduct(MEDICATION, ONE);

        Act prescription = PrescriptionTestHelper.createPrescription(patient, product1, clinician);
        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        CustomerChargeActItemEditor itemEditor1 = addItem(editor, patient, product1, ONE, queue);
        addItem(editor, patient, product2, ONE, queue);
        assertTrue(SaveHelper.save(editor));

        checkPrescription(prescription, itemEditor1);
        editor.removeItem((Act) itemEditor1.getObject());
        assertTrue(SaveHelper.save(editor));

        prescription = get(prescription);
        ActBean bean = new ActBean(prescription);
        assertTrue(bean.getNodeActs("dispensing").isEmpty());
    }

    /**
     * Verifies that an unsaved invoice item can be deleted, when there is a prescription associated with the item's
     * product.
     */
    @Test
    public void testDeleteUnsavedInvoiceItemWithPrescription() {
        checkDeleteUnsavedItemWithPrescription((FinancialAct) create(CustomerAccountArchetypes.INVOICE));
    }

    /**
     * Verifies that an unsaved credit item can be deleted, when there is a prescription associated with the item's
     * product. As it is a credit item, the prescription shouldn't be used.
     */
    @Test
    public void testDeleteUnsavedCreditItem() {
        checkDeleteUnsavedItemWithPrescription((FinancialAct) create(CustomerAccountArchetypes.CREDIT));
    }

    /**
     * Verifies that an unsaved counter item can be deleted, when there is a prescription associated with the item's
     * product. As it is a counter item, the prescription shouldn't be used.
     */
    @Test
    public void testDeleteUnsavedCounterItem() {
        checkDeleteUnsavedItemWithPrescription((FinancialAct) create(CustomerAccountArchetypes.COUNTER));
    }

    /**
     * Verifies that the clinician is propagated to child acts.
     */
    @Test
    public void testInitClinician() {
        Product product1 = createProduct(MEDICATION, BigDecimal.ONE);
        Entity reminderType1 = addReminder(product1);
        Entity investigationType1 = addInvestigation(product1);
        Entity template1 = addTemplate(product1);

        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        BigDecimal quantity = ONE;
        CustomerChargeActItemEditor itemEditor = addItem(editor, patient, product1, quantity, queue);
        FinancialAct item = (FinancialAct) itemEditor.getObject();

        assertTrue(editor.isValid());
        assertTrue(SaveHelper.save(editor));

        Act medication = (Act) new ActBean(item).getNodeTargetObject("dispensing");
        assertNotNull(medication);

        Act reminder = getReminder(item, reminderType1);
        Act investigation = getInvestigation(item, investigationType1);
        Act document = getDocument(item, template1);
        checkClinician(medication, clinician);
        checkClinician(reminder, clinician);
        checkClinician(investigation, clinician);
        checkClinician(document, clinician);
    }

    /**
     * Verifies that the patient on an invoice item can be changed, and that this is reflected in the patient history.
     */
    @Test
    public void testChangePatient() {
        Product product1 = createProduct(MEDICATION, BigDecimal.ONE);
        Entity investigationType1 = addInvestigation(product1);
        Entity template1 = addTemplate(product1);

        Party patient2 = TestHelper.createPatient(customer);

        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);
        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        BigDecimal quantity = ONE;
        CustomerChargeActItemEditor itemEditor = addItem(editor, patient, product1, quantity, queue);
        FinancialAct item = (FinancialAct) itemEditor.getObject();

        assertTrue(editor.isValid());
        assertTrue(SaveHelper.save(editor));

        Act event1 = records.getEvent(patient);  // get the clinical event
        Act medication = (Act) new ActBean(item).getNodeTargetObject("dispensing");
        assertNotNull(event1);
        assertNotNull(medication);

        Act investigation = getInvestigation(item, investigationType1);
        Act document = getDocument(item, template1);

        checkEventRelationships(event1, item, medication, investigation, document);

        // recreate the editor, and change the patient to patient2.
        editor = createCustomerChargeActEditor(charge, layoutContext);
        editor.getComponent();
        assertTrue(editor.isValid());

        item = (FinancialAct) editor.getItems().getActs().get(0);
        itemEditor = editor.getEditor(item);
        itemEditor.getComponent();
        itemEditor.setPatient(patient2);
        assertTrue(editor.isValid());
        assertTrue(SaveHelper.save(editor));

        event1 = get(event1);
        medication = get(medication);

        // verify that the records are now linked to event2
        Act event2 = records.getEvent(patient2);
        investigation = getInvestigation(item, investigationType1);
        document = getDocument(item, template1);

        checkEventRelationships(event2, item, medication, investigation, document);

        // event1 should no longer be linked to any acts
        assertEquals(0, event1.getSourceActRelationships().size());
    }

    /**
     * Verifies that changing a quantity on a pharmacy order sends an update.
     */
    @Test
    public void testChangePharmacyOrderQuantity() {
        Entity pharmacy = createPharmacy(location);
        Product product = createProduct(MEDICATION, TEN, pharmacy);

        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        CustomerChargeActItemEditor itemEditor = addItem(editor, patient, product, TEN, queue);
        Act item = (Act) itemEditor.getObject();
        assertTrue(SaveHelper.save(editor));

        List<Order> orders = editor.getPharmacyOrderService().getOrders(true);
        assertEquals(1, orders.size());
        checkOrder(orders.get(0), Order.Type.CREATE, patient, product, TEN, item.getId(),
                   item.getActivityStartTime(), clinician, pharmacy);
        editor.getPharmacyOrderService().clear();

        itemEditor.setQuantity(ONE);
        assertTrue(SaveHelper.save(editor));

        orders = editor.getPharmacyOrderService().getOrders(true);
        assertEquals(1, orders.size());
        checkOrder(orders.get(0), Order.Type.UPDATE, patient, product, ONE, item.getId(),
                   item.getActivityStartTime(), clinician, pharmacy);
    }

    /**
     * Verifies that deleting an invoice item with a pharmacy order cancels the order.
     */
    @Test
    public void testDeleteInvoiceItemWithPharmacyOrder() {
        Entity pharmacy = createPharmacy(location);
        Product product1 = createProduct(MEDICATION, TEN, pharmacy);
        Product product2 = createProduct(ProductArchetypes.MERCHANDISE, TEN, pharmacy);

        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        Act item1 = (Act) addItem(editor, patient, product1, TEN, queue).getObject();
        Act item2 = (Act) addItem(editor, patient, product2, ONE, queue).getObject();
        assertTrue(SaveHelper.save(editor));

        List<Order> orders = editor.getPharmacyOrderService().getOrders(true);
        assertEquals(2, orders.size());
        checkOrder(orders.get(0), Order.Type.CREATE, patient, product1, TEN, item1.getId(),
                   item1.getActivityStartTime(), clinician, pharmacy);
        checkOrder(orders.get(1), Order.Type.CREATE, patient, product2, ONE, item2.getId(),
                   item2.getActivityStartTime(), clinician, pharmacy);
        editor.getPharmacyOrderService().clear();

        editor.delete(item1);
        assertTrue(SaveHelper.save(editor));

        orders = editor.getPharmacyOrderService().getOrders(true);
        assertEquals(1, orders.size());
        checkOrder(orders.get(0), Order.Type.CANCEL, patient, product1, TEN, item1.getId(),
                   item1.getActivityStartTime(), clinician, pharmacy);
    }

    /**
     * Tests expansion for invoices.
     */
    @Test
    public void testTemplateExpansionForInvoice() {
        checkTemplateExpansion(CustomerAccountArchetypes.INVOICE, 1);
    }

    /**
     * Tests expansion for credits.
     */
    @Test
    public void testTemplateExpansionForCredit() {
        checkTemplateExpansion(CustomerAccountArchetypes.CREDIT, 0);
    }

    /**
     * Tests expansion for counter sales.
     */
    @Test
    public void testTemplateExpansionForCounter() {
        checkTemplateExpansion(CustomerAccountArchetypes.COUNTER, 0);
    }

    /**
     * Verifies that if a template is expanded, and a product is subsequently replaced with one not from a template,
     * the template reference is removed.
     */
    @Test
    public void testChangeTemplateProduct() {
        BigDecimal fixedPrice = ONE;

        Product template = ProductTestHelper.createTemplate("templateA");
        Product product1 = createProduct(MEDICATION, fixedPrice);
        Product product2 = createProduct(MEDICATION, fixedPrice);

        ProductTestHelper.addInclude(template, product1, 1, false);

        FinancialAct charge = (FinancialAct) create(CustomerAccountArchetypes.INVOICE);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        CustomerChargeActItemEditor itemEditor = addItem(editor, patient, template, null, queue);
        assertEquals(product1, itemEditor.getProduct());
        assertEquals(template, itemEditor.getTemplate());

        itemEditor.setProduct(product2);
        assertNull(itemEditor.getTemplate());

        assertTrue(SaveHelper.save(editor));

        itemEditor.setProduct(template);
        assertEquals(product1, itemEditor.getProduct());
        assertEquals(template, itemEditor.getTemplate());
    }

    /**
     * Tests template expansion.
     *
     * @param shortName the charge short name
     * @param childActs the expected no. of child acts
     */
    private void checkTemplateExpansion(String shortName, int childActs) {
        BigDecimal fixedPrice = ONE;
        Entity discount = DiscountTestHelper.createDiscount(BigDecimal.TEN, true, DiscountRules.PERCENTAGE);

        Product template = ProductTestHelper.createTemplate("templateA");
        Product product1 = createProduct(MEDICATION, fixedPrice);
        Product product2 = createProduct(MEDICATION, fixedPrice);
        Product product3 = createProduct(MEDICATION, fixedPrice);
        addDiscount(product3, discount);
        addDiscount(customer, discount);                           // give customer a discount for product3
        ProductTestHelper.addInclude(template, product1, 1, false);
        ProductTestHelper.addInclude(template, product2, 2, false);
        ProductTestHelper.addInclude(template, product3, 3, true); // zero price

        FinancialAct charge = (FinancialAct) create(shortName);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        addItem(editor, patient, template, null, queue);
        assertTrue(SaveHelper.save(editor));

        charge = get(charge);
        ActBean bean = new ActBean(charge);
        List<FinancialAct> items = bean.getNodeActs("items", FinancialAct.class);
        assertEquals(3, items.size());

        checkItem(items, patient, product1, template, author, clinician, ONE, ZERO, ZERO, ZERO, ONE, ZERO,
                  new BigDecimal("0.091"), ONE, null, childActs);
        checkItem(items, patient, product2, template, author, clinician, BigDecimal.valueOf(2), ZERO, ZERO, ZERO, ONE,
                  ZERO, new BigDecimal("0.091"), ONE, null, childActs);

        // verify that product3 is charged at zero price
        checkItem(items, patient, product3, template, author, clinician, BigDecimal.valueOf(3), ZERO, ZERO, ZERO, ZERO,
                  ZERO, ZERO, ZERO, null, childActs);
    }

    /**
     * Verifies that an unsaved charge item can be deleted, when there is a prescription associated with the item's
     * product.
     */
    private void checkDeleteUnsavedItemWithPrescription(FinancialAct charge) {
        Product product1 = createProduct(MEDICATION, ONE);
        Product product2 = createProduct(MEDICATION, ONE);

        Act prescription = PrescriptionTestHelper.createPrescription(patient, product1, clinician);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        // add items for product1 and product2, but delete product1 item before save
        CustomerChargeActItemEditor itemEditor1 = addItem(editor, patient, product1, ONE, queue);
        addItem(editor, patient, product2, ONE, queue);
        editor.removeItem((Act) itemEditor1.getObject());
        assertTrue(editor.isValid());
        assertTrue(SaveHelper.save(editor));

        // verify there are no acts linked to the prescription
        prescription = get(prescription);
        ActBean bean = new ActBean(prescription);
        assertTrue(bean.getNodeActs("dispensing").isEmpty());

        // reload the charge and verify the editor is valid
        charge = get(charge);
        editor = createCustomerChargeActEditor(charge, layoutContext);
        editor.getComponent();
        assertTrue(editor.isValid());
    }

    /**
     * Verifies that the {@link CustomerChargeActEditor#delete()} method deletes a charge and its items.
     *
     * @param charge the charge
     */
    private void checkDeleteCharge(FinancialAct charge) {
        BigDecimal fixedPrice = BigDecimal.valueOf(11);
        BigDecimal itemTotal = BigDecimal.valueOf(11);
        BigDecimal total = itemTotal.multiply(BigDecimal.valueOf(3));

        Product product1 = createProduct(MEDICATION, fixedPrice);
        Product product2 = createProduct(ProductArchetypes.MERCHANDISE, fixedPrice);
        Product product3 = createProduct(ProductArchetypes.SERVICE, fixedPrice);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        BigDecimal quantity = ONE;
        addItem(editor, patient, product1, quantity, queue);
        addItem(editor, patient, product2, quantity, queue);
        addItem(editor, patient, product3, quantity, queue);

        assertTrue(SaveHelper.save(editor));
        BigDecimal balance = (charge.isCredit()) ? total.negate() : total;
        checkBalance(customer, balance, ZERO);

        charge = get(charge);
        ActBean bean = new ActBean(charge);
        List<FinancialAct> items = bean.getNodeActs("items", FinancialAct.class);
        assertEquals(3, items.size());

        assertTrue(delete(editor));
        assertNull(get(charge));
        for (FinancialAct item : items) {
            assertNull(get(item));
        }

        checkBalance(customer, ZERO, ZERO);
    }

    /**
     * Tests editing a charge with no items.
     *
     * @param charge the charge
     */
    private void checkEmptyCharge(FinancialAct charge) {
        CustomerChargeActEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        editor.getComponent();
        assertTrue(editor.isValid());
        assertTrue(editor.save());
        checkBalance(customer, ZERO, ZERO);

        editor.setStatus(ActStatus.POSTED);
        assertTrue(editor.save());
        checkBalance(customer, ZERO, ZERO);
    }

    /**
     * Tests editing of a charge.
     *
     * @param charge the charge
     */
    private void checkEditCharge(FinancialAct charge) {
        Party location = TestHelper.createLocation(true);   // enable stock control
        Party stockLocation = createStockLocation(location);
        layoutContext.getContext().setLocation(location);
        layoutContext.getContext().setStockLocation(stockLocation);

        BigDecimal fixedPrice = BigDecimal.valueOf(11);
        BigDecimal itemTax = BigDecimal.valueOf(1);
        BigDecimal itemTotal = BigDecimal.valueOf(11);
        BigDecimal tax = itemTax.multiply(BigDecimal.valueOf(3));
        BigDecimal total = itemTotal.multiply(BigDecimal.valueOf(3));
        Entity pharmacy = CustomerChargeTestHelper.createPharmacy(location);

        boolean invoice = TypeHelper.isA(charge, CustomerAccountArchetypes.INVOICE);

        Product product1 = createProduct(MEDICATION, fixedPrice, pharmacy);
        addReminder(product1);
        addInvestigation(product1);
        addTemplate(product1);
        int product1Acts = invoice ? 4 : 0;

        Product product2 = createProduct(ProductArchetypes.MERCHANDISE, fixedPrice, pharmacy);
        addReminder(product2);
        addInvestigation(product2);
        addTemplate(product2);
        int product2Acts = invoice ? 3 : 0;

        Product product3 = createProduct(ProductArchetypes.SERVICE, fixedPrice);
        addReminder(product3);
        addInvestigation(product3);
        addTemplate(product3);
        int product3Acts = invoice ? 3 : 0;

        BigDecimal product1Stock = BigDecimal.valueOf(100);
        BigDecimal product2Stock = BigDecimal.valueOf(50);
        initStock(product1, stockLocation, product1Stock);
        initStock(product2, stockLocation, product2Stock);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        ChargeEditorQueue queue = editor.getQueue();
        editor.getComponent();
        assertTrue(editor.isValid());

        BigDecimal quantity = ONE;
        Act item1 = (Act) addItem(editor, patient, product1, quantity, queue).getObject();
        Act item2 = (Act) addItem(editor, patient, product2, quantity, queue).getObject();
        addItem(editor, patient, product3, quantity, queue);

        assertTrue(editor.isValid());
        assertTrue(SaveHelper.save(editor));
        BigDecimal balance = (charge.isCredit()) ? total.negate() : total;
        checkBalance(customer, balance, ZERO);
        editor.setStatus(ActStatus.POSTED);
        assertTrue(editor.save());
        checkBalance(customer, ZERO, balance);

        if (invoice) {
            List<Order> orders = editor.getPharmacyOrderService().getOrders(true);
            assertEquals(4, orders.size());
            checkOrder(orders.get(0), Order.Type.CREATE, patient, product1, quantity, item1.getId(),
                       item1.getActivityStartTime(), clinician, pharmacy);
            checkOrder(orders.get(1), Order.Type.DISCONTINUE, patient, product1, quantity, item1.getId(),
                       item1.getActivityStartTime(), clinician, pharmacy);
            checkOrder(orders.get(2), Order.Type.CREATE, patient, product2, quantity, item2.getId(),
                       item2.getActivityStartTime(), clinician, pharmacy);
            checkOrder(orders.get(3), Order.Type.DISCONTINUE, patient, product2, quantity, item2.getId(),
                       item2.getActivityStartTime(), clinician, pharmacy);
            editor.getPharmacyOrderService().clear();
        } else {
            assertNull(editor.getPharmacyOrderService());
        }

        charge = get(charge);
        ActBean bean = new ActBean(charge);
        List<FinancialAct> items = bean.getNodeActs("items", FinancialAct.class);
        assertEquals(3, items.size());
        checkCharge(charge, customer, author, clinician, tax, total);

        Act event = records.getEvent(patient);  // get the clinical event. Should be null if not an invoice
        if (invoice) {
            assertNotNull(event);
            checkEvent(event, patient, author, clinician, location);
        } else {
            assertNull(event);
        }

        BigDecimal discount = ZERO;
        checkItem(items, patient, product1, null, author, clinician, quantity, ZERO, ZERO,
                  ZERO, fixedPrice, discount, itemTax, itemTotal, event, product1Acts);
        checkItem(items, patient, product2, null, author, clinician, quantity, ZERO, ZERO,
                  ZERO, fixedPrice, discount, itemTax, itemTotal, event, product2Acts);
        checkItem(items, patient, product3, null, author, clinician, quantity, ZERO, ZERO,
                  ZERO, fixedPrice, discount, itemTax, itemTotal, event, product3Acts);

        boolean add = bean.isA(CustomerAccountArchetypes.CREDIT);
        checkStock(product1, stockLocation, product1Stock, quantity, add);
        checkStock(product2, stockLocation, product2Stock, quantity, add);
        checkStock(product3, stockLocation, ZERO, ZERO, add);
    }

    /**
     * Verifies stock quantities update for products used in a charge.
     *
     * @param charge the charge
     */
    private void checkChargeStockUpdate(FinancialAct charge) {
        Party location = TestHelper.createLocation(true);   // enable stock control
        Party stockLocation = createStockLocation(location);
        layoutContext.getContext().setLocation(location);
        layoutContext.getContext().setStockLocation(stockLocation);

        Product product1 = createProduct(MEDICATION);
        Product product2 = createProduct(ProductArchetypes.MERCHANDISE);
        Product product3 = createProduct(ProductArchetypes.SERVICE);
        Product product4 = createProduct(ProductArchetypes.MERCHANDISE);

        BigDecimal product1InitialStock = BigDecimal.valueOf(100);
        BigDecimal product2InitialStock = BigDecimal.valueOf(50);
        BigDecimal product4InitialStock = BigDecimal.valueOf(25);
        initStock(product1, stockLocation, product1InitialStock);
        initStock(product2, stockLocation, product2InitialStock);
        initStock(product4, stockLocation, product4InitialStock);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        editor.getComponent();
        assertTrue(editor.isValid());

        BigDecimal quantity1 = BigDecimal.valueOf(5);
        BigDecimal quantity2 = BigDecimal.TEN;
        BigDecimal quantity4a = BigDecimal.ONE;
        BigDecimal quantity4b = BigDecimal.TEN;
        BigDecimal quantity4 = quantity4a.add(quantity4b);

        CustomerChargeActItemEditor item1 = addItem(editor, patient, product1, quantity1, editor.getQueue());
        CustomerChargeActItemEditor item2 = addItem(editor, patient, product2, quantity2, editor.getQueue());
        addItem(editor, patient, product4, quantity4a, editor.getQueue());
        addItem(editor, patient, product4, quantity4b, editor.getQueue());
        assertTrue(SaveHelper.save(editor));

        boolean add = TypeHelper.isA(charge, CustomerAccountArchetypes.CREDIT);
        BigDecimal product1Stock = checkStock(product1, stockLocation, product1InitialStock, quantity1, add);
        BigDecimal product2Stock = checkStock(product2, stockLocation, product2InitialStock, quantity2, add);
        checkStock(product4, stockLocation, product4InitialStock, quantity4, add);

        item1.setQuantity(ZERO);
        item1.setQuantity(quantity1);
        assertTrue(item1.isModified());
        item2.setQuantity(ZERO);
        item2.setQuantity(quantity2);
        assertTrue(item2.isModified());
        assertTrue(SaveHelper.save(editor));
        checkStock(product1, stockLocation, product1Stock);
        checkStock(product2, stockLocation, product2Stock);

        item1.setQuantity(BigDecimal.valueOf(10)); // change product1 stock quantity
        item2.setProduct(product3);                // change the product and verify the stock for product2 reverts
        assertTrue(SaveHelper.save(editor));
        checkStock(product1, stockLocation, product1Stock, BigDecimal.valueOf(5), add);
        checkStock(product2, stockLocation, product2Stock, quantity2, !add);

        // now delete the charge and verify the stock reverts
        assertTrue(delete(editor));
        checkStock(product1, stockLocation, product1InitialStock);
        checkStock(product2, stockLocation, product2InitialStock);
        checkStock(product4, stockLocation, product4InitialStock);
    }

    /**
     * Tests template expansion.
     *
     * @param charge the charge to edit
     */
    private void checkExpandTemplate(FinancialAct charge) {
        Party location = TestHelper.createLocation(true);   // enable stock control
        Party stockLocation = createStockLocation(location);
        layoutContext.getContext().setLocation(location);
        layoutContext.getContext().setStockLocation(stockLocation);

        BigDecimal quantity = BigDecimal.valueOf(1);
        BigDecimal fixedPrice = BigDecimal.valueOf(11);
        BigDecimal discount = ZERO;
        BigDecimal itemTax = BigDecimal.valueOf(1);
        BigDecimal itemTotal = BigDecimal.valueOf(11);
        BigDecimal tax = itemTax.multiply(BigDecimal.valueOf(3));
        BigDecimal total = itemTotal.multiply(BigDecimal.valueOf(3));

        Product product1 = createProduct(MEDICATION, fixedPrice);
        Product product2 = createProduct(ProductArchetypes.MERCHANDISE, fixedPrice);
        Product product3 = createProduct(ProductArchetypes.SERVICE, fixedPrice);
        Product template = createProduct(ProductArchetypes.TEMPLATE);
        EntityBean templateBean = new EntityBean(template);
        templateBean.addNodeTarget("includes", product1);
        templateBean.addNodeTarget("includes", product2);
        templateBean.addNodeTarget("includes", product3);
        save(template);

        TestChargeEditor editor = createCustomerChargeActEditor(charge, layoutContext);
        editor.getComponent();
        CustomerChargeTestHelper.addItem(editor, patient, template, ONE, editor.getQueue());

        boolean invoice = TypeHelper.isA(charge, CustomerAccountArchetypes.INVOICE);
        int product1Acts = 0;     // expected child acts for product1
        if (invoice) {
            product1Acts++;
        }

        assertTrue(SaveHelper.save(editor));
        BigDecimal balance = (charge.isCredit()) ? total.negate() : total;
        checkBalance(customer, balance, ZERO);

        charge = get(charge);
        ActBean bean = new ActBean(charge);
        List<FinancialAct> items = bean.getNodeActs("items", FinancialAct.class);
        assertEquals(3, items.size());
        checkCharge(charge, customer, author, clinician, tax, total);
        Act event = records.getEvent(patient);  // get the clinical event. Should be null if not an invoice
        if (invoice) {
            assertNotNull(event);
            checkEvent(event, patient, author, clinician, location);
        } else {
            assertNull(event);
        }

        checkItem(items, patient, product1, template, author, clinician, quantity, ZERO, ZERO,
                  ZERO, fixedPrice, discount, itemTax, itemTotal, event, product1Acts);
        checkItem(items, patient, product2, template, author, clinician, quantity, ZERO, ZERO,
                  ZERO, fixedPrice, discount, itemTax, itemTotal, event, 0);
        checkItem(items, patient, product3, template, author, clinician, quantity, ZERO, ZERO,
                  ZERO, fixedPrice, discount, itemTax, itemTotal, event, 0);
    }

    /**
     * Initialises stock quantities for a product at a stock location.
     *
     * @param product       the product
     * @param stockLocation the stock location
     * @param quantity      the initial stock quantity
     */
    private void initStock(Product product, Party stockLocation, BigDecimal quantity) {
        StockRules rules = new StockRules(getArchetypeService());
        rules.updateStock(product, stockLocation, quantity);
    }

    /**
     * Checks stock for a product at a stock location.
     *
     * @param product       the product
     * @param stockLocation the stock location
     * @param initial       the initial stock quantity
     * @param change        the change in stock quantity
     * @param add           if {@code true} add the change, otherwise subtract it
     * @return the new stock quantity
     */
    private BigDecimal checkStock(Product product, Party stockLocation, BigDecimal initial, BigDecimal change,
                                  boolean add) {
        BigDecimal expected = add ? initial.add(change) : initial.subtract(change);
        checkStock(product, stockLocation, expected);
        return expected;
    }

    /**
     * Checks stock for a product at a stock location.
     *
     * @param product       the product
     * @param stockLocation the stock location
     * @param expected      the expected stock quantity
     */
    private void checkStock(Product product, Party stockLocation, BigDecimal expected) {
        StockRules rules = new StockRules(getArchetypeService());
        checkEquals(expected, rules.getStock(get(product), get(stockLocation)));
    }

    /**
     * Verifies a patient clinical event matches that expected.
     *
     * @param event     the event
     * @param patient   the expected patient
     * @param author    the expected author
     * @param clinician the expected clinician
     * @param location  the expected location
     */
    private void checkEvent(Act event, Party patient, User author, User clinician, Party location) {
        ActBean bean = new ActBean(event);
        assertEquals(patient.getObjectReference(), bean.getNodeParticipantRef("patient"));
        assertEquals(author.getObjectReference(), bean.getNodeParticipantRef("author"));
        assertEquals(clinician.getObjectReference(), bean.getNodeParticipantRef("clinician"));
        assertEquals(location.getObjectReference(), bean.getNodeParticipantRef("location"));
    }

    /**
     * Verifies a prescription has a link to a charge item
     *
     * @param prescription the prescription
     * @param itemEditor   the charge item editor
     */
    private void checkPrescription(Act prescription, CustomerChargeActItemEditor itemEditor) {
        prescription = get(prescription);
        assertNotNull(prescription);
        ActBean prescriptionBean = new ActBean(prescription);
        Act item = (Act) itemEditor.getObject();
        ActBean bean = new ActBean(item);
        List<Act> dispensing = bean.getNodeActs("dispensing");
        assertEquals(1, dispensing.size());
        Act medication = dispensing.get(0);
        assertTrue(prescriptionBean.getNodeActs("dispensing").contains(medication));
    }

    /**
     * Deletes a charge.
     *
     * @param editor the editor to use
     * @return {@code true} if the delete was successful
     */
    private boolean delete(final CustomerChargeActEditor editor) {
        TransactionTemplate template = new TransactionTemplate(ServiceHelper.getTransactionManager());
        return template.execute(new TransactionCallback<Boolean>() {
            public Boolean doInTransaction(TransactionStatus status) {
                return editor.delete();
            }
        });
    }

    /**
     * Checks the balance for a customer.
     *
     * @param customer the customer
     * @param unbilled the expected unbilled amount
     * @param balance  the expected balance
     */
    private void checkBalance(Party customer, BigDecimal unbilled, BigDecimal balance) {
        CustomerAccountRules rules = ServiceHelper.getBean(CustomerAccountRules.class);
        checkEquals(unbilled, rules.getUnbilledAmount(customer));
        checkEquals(balance, rules.getBalance(customer));
    }

    /**
     * Chekcs the total of a charge matches that expected, and that the total matches the sum of the item totals.
     *
     * @param charge the charge
     * @param total  the expected total
     */
    private void checkTotal(FinancialAct charge, BigDecimal total) {
        assertTrue(charge.getTotal().compareTo(total) == 0);
        ActCalculator calculator = new ActCalculator(getArchetypeService());
        BigDecimal itemTotal = calculator.sum(charge, "total");
        assertTrue(itemTotal.compareTo(total) == 0);
    }

    /**
     * Creates a customer charge act editor.
     *
     * @param invoice the charge to edit
     * @param context the layout context
     * @return a new customer charge act editor
     */
    private TestChargeEditor createCustomerChargeActEditor(FinancialAct invoice, LayoutContext context) {
        return createCustomerChargeActEditor(invoice, context, false);
    }

    /**
     * Creates a customer charge act editor.
     *
     * @param invoice        the charge to edit
     * @param context        the layout context
     * @param addDefaultItem if {@code true} add a default item if the act has none
     * @return a new customer charge act editor
     */
    private TestChargeEditor createCustomerChargeActEditor(FinancialAct invoice, LayoutContext context,
                                                           boolean addDefaultItem) {
        return new TestChargeEditor(invoice, context, addDefaultItem);
    }

    /**
     * Helper to create a new stock location, linked to a location.
     *
     * @param location the location
     * @return the stock location
     */
    private Party createStockLocation(Party location) {
        Party stockLocation = (Party) create(StockArchetypes.STOCK_LOCATION);
        stockLocation.setName("STOCK-LOCATION-" + stockLocation.hashCode());
        EntityBean locationBean = new EntityBean(location);
        locationBean.addRelationship("entityRelationship.locationStockLocation", stockLocation);
        save(location, stockLocation);
        return stockLocation;
    }

}
