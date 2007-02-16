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
 *  Copyright 2006 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.web.component.im.edit.payment;

import org.openvpms.archetype.rules.balance.CustomerBalanceRules;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.service.archetype.helper.TypeHelper;
import org.openvpms.web.component.edit.Property;
import org.openvpms.web.component.im.edit.AbstractIMObjectEditor;
import org.openvpms.web.component.im.edit.IMObjectEditor;
import org.openvpms.web.component.im.edit.act.ActHelper;
import org.openvpms.web.component.im.layout.LayoutContext;

import java.math.BigDecimal;


/**
 * An editor for {@link Act}s which have an archetype in
 * <em>act.customerAccountPayment*</em>, and <em>act.customerAccountRefund*</em>
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate:2006-02-21 03:48:29Z $
 */
public class CustomerPaymentItemEditor extends AbstractIMObjectEditor {

    /**
     * Construct a new <code>CustomerPaymentItemEditor</code>.
     *
     * @param act     the act to edit
     * @param parent  the parent act
     * @param context the layout context
     */
    public CustomerPaymentItemEditor(Act act, Act parent,
                                     LayoutContext context) {
        super(act, parent, context);
        if (act.isNew()) {
            // Default the amount to the outstanding balance
            // If balance < 0 set to 0.00 for payments, or -balance for refunds
            // If balance > 0 set to 0.00 for refunds
            boolean payment = TypeHelper.isA(act,
                                             "act.customerAccountPayment*");
            Party customer = context.getContext().getCustomer();
            if (customer != null) {
                BigDecimal diff = ActHelper.sum(parent, "amount");
                CustomerBalanceRules rules = new CustomerBalanceRules();
                BigDecimal current = rules.getBalance(customer);
                BigDecimal balance = current.subtract(diff);
                if (balance.signum() == -1) {
                    balance = (payment) ? BigDecimal.ZERO : balance.negate();
                } else if (balance.signum() == 1 && !payment) {
                    balance = BigDecimal.ZERO;
                }
                Property amount = getProperty("amount");
                amount.setValue(balance);
            }
        }
    }

    /**
     * Create a new editor for an object, if it can be edited by this class.
     *
     * @param object  the object to edit
     * @param parent  the parent object. May be <code>null</code>
     * @param context the layout context
     * @return a new editor for <code>object</code>, or <code>null</code> if it
     *         cannot be edited by this
     */
    public static IMObjectEditor create(IMObject object, IMObject parent,
                                        LayoutContext context) {
        IMObjectEditor result = null;
        if (TypeHelper.isA(object, "act.customerAccountPayment*",
                           "act.customerAccountRefund*")
                && !TypeHelper.isA(object, "act.customerAccountPayment",
                                   "act.customerAccountRefund")
                && parent instanceof Act) {
            result = new CustomerPaymentItemEditor((Act) object, (Act) parent,
                                                   context);
        }
        return result;
    }

}
