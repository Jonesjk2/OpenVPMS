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
package org.openvpms.web.workspace.customer.info;

import org.openvpms.archetype.rules.customer.CustomerArchetypes;
import org.openvpms.archetype.rules.util.DateRules;
import org.openvpms.archetype.rules.util.DateUnits;
import org.openvpms.archetype.rules.workflow.ScheduleArchetypes;
import org.openvpms.archetype.rules.workflow.WorkflowStatus;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.web.component.im.query.ActStatuses;
import org.openvpms.web.component.im.query.DateRangeActQuery;

import java.util.Date;

/**
 * Customer appointment query.
 *
 * @author Tim Anderson
 */
public class CustomerAppointmentQuery extends DateRangeActQuery<Act> {

    /**
     * The act statuses to query.
     */
    private static final ActStatuses STATUSES;

    static {
        STATUSES = new ActStatuses(ScheduleArchetypes.APPOINTMENT);
        STATUSES.setDefault(WorkflowStatus.PENDING);
    }

    /**
     * Constructs a {@link CustomerAppointmentQuery}.
     *
     * @param customer the customer
     */
    public CustomerAppointmentQuery(Party customer) {
        super(customer, "customer", CustomerArchetypes.CUSTOMER_PARTICIPATION,
              new String[]{ScheduleArchetypes.APPOINTMENT}, STATUSES, Act.class);

        getComponent(); // force creation of component to enable initialisation of fields

        // default the date range from a month in the past to 2 years into the future
        Date now = new Date();
        setFrom(DateRules.getDate(now, -1, DateUnits.MONTHS));
        setTo(DateRules.getDate(now, 2, DateUnits.YEARS));
    }

}
