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
 * Copyright 2013 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.rules.workflow;

import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.system.common.query.ObjectSet;

import java.util.Date;


/**
 * Queries <em>act.customerTask</em> acts, returning a limited set of data for display purposes.
 *
 * @author Tim Anderson
 */
class TaskQuery extends ScheduleEventQuery {

    /**
     * Constructs a {@link TaskQuery}.
     *
     * @param workList the schedule
     * @param from     the 'from' start time
     * @param to       the 'to' start time
     * @param service  the archetype service
     */
    public TaskQuery(Entity workList, Date from, Date to, IArchetypeService service) {
        super(workList, from, to, ScheduleArchetypes.TASK, service);
    }

    /**
     * Returns the name of the named query to execute.
     *
     * @return the name of the named query
     */
    protected String getQueryName() {
        return "act.customerTask";
    }

    /**
     * Returns the archetype short name of the schedule type.
     *
     * @return the short name of the schedule type
     */
    protected String getScheduleType() {
        return ScheduleArchetypes.TASK_TYPE;
    }

    /**
     * Creates a new {@link ObjectSet} representing a scheduled event.
     *
     * @param actRef the reference of the event act
     * @param set    the source set
     * @return a new event
     */
    @Override
    protected ObjectSet createEvent(IMObjectReference actRef, ObjectSet set) {
        ObjectSet event = super.createEvent(actRef, set);
        event.set(ScheduleEvent.CONSULT_START_TIME, null);
        return event;
    }

}
