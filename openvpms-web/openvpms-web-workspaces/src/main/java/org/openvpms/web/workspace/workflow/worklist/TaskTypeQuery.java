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
 */

package org.openvpms.web.workspace.workflow.worklist;

import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.web.component.app.Context;
import org.openvpms.web.workspace.workflow.ScheduleTypeQuery;


/**
 * Task type query.
 *
 * @author Tim Anderson
 */
public class TaskTypeQuery extends ScheduleTypeQuery {

    /**
     * Constructs a new {@code TaskTypeQuery} that queries IMObjects with
     * the specified criteria.
     *
     * @param workList the schedule. May be {@code null}
     * @param context  the context
     */
    public TaskTypeQuery(Party workList, Context context) {
        super(new String[]{"entity.taskType"}, workList, "taskTypes", context);
    }

}
