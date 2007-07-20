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

package org.openvpms.web.app.patient.mr;

import nextapp.echo2.app.Component;
import static org.openvpms.archetype.rules.act.ActStatus.COMPLETED;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.archetype.descriptor.NodeDescriptor;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.web.component.im.edit.act.AbstractActEditor;
import org.openvpms.web.component.im.layout.AbstractLayoutStrategy;
import org.openvpms.web.component.im.layout.IMObjectLayoutStrategy;
import org.openvpms.web.component.im.layout.LayoutContext;
import org.openvpms.web.component.property.Modifiable;
import org.openvpms.web.component.property.ModifiableListener;
import org.openvpms.web.component.property.Property;
import org.openvpms.web.component.property.PropertySet;
import org.openvpms.web.component.util.ColumnFactory;
import org.openvpms.web.component.util.TabPaneModel;
import org.openvpms.web.resource.util.Messages;

import java.util.Date;
import java.util.List;


/**
 * Editor for <em>act.patientClinicalEvent</em> acts.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class PatientClinicalEventActEditor extends AbstractActEditor {

    /**
     * Constructs a new <code>PatientClinicalEventActEditor</code>.
     *
     * @param act     the act to edit
     * @param parent  the parent object. May be <code>null</code>
     * @param context the layout context
     */
    public PatientClinicalEventActEditor(Act act, IMObject parent,
                                         LayoutContext context) {
        super(act, parent, context);
        initParticipant("customer", context.getContext().getCustomer());
        initParticipant("patient", context.getContext().getPatient());
        initParticipant("worklist", context.getContext().getWorkList());

        addStartEndTimeListeners();

        getProperty("status").addModifiableListener(new ModifiableListener() {
            public void modified(Modifiable modifiable) {
                onStatusChanged();
            }
        });
    }

    /**
     * Invoked when the status changes. Sets the end time to today if the
     * status is 'Completed', or <code>null</code> if it is 'Pending'.
     */
    private void onStatusChanged() {
        Property status = getProperty("status");
        String value = (String) status.getValue();
        Date time = COMPLETED.equals(value) ? new Date() : null;
        setEndTime(time, false);
    }

    /**
     * Creates the layout strategy.
     *
     * @return a new layout strategy
     */
    @Override
    protected IMObjectLayoutStrategy createLayoutStrategy() {
        return new LayoutStrategy();
    }

    class LayoutStrategy extends AbstractLayoutStrategy {

        /**
         * Lays out child components in a tab model.
         *
         * @param object      the parent object
         * @param descriptors the property descriptors
         * @param properties  the properties
         * @param context     the layout context
         * @return the tab model
         */
        @Override
        protected TabPaneModel doTabLayout(IMObject object,
                                           List<NodeDescriptor> descriptors,
                                           PropertySet properties,
                                           Component container,
                                           LayoutContext context) {
            TabPaneModel model = super.doTabLayout(object, descriptors,
                                                   properties, container,
                                                   context);
            Party patient = (Party) getParticipant("patient");
            if (patient != null) {
                PatientSummaryQuery query = new PatientSummaryQuery(patient);
                SummaryTableBrowser browser = new SummaryTableBrowser(query);
                String title = Messages.get("button.summary");
                Component inset = ColumnFactory.create("Inset",
                                                       browser.getComponent());
                model.addTab(title, inset);
            }
            return model;
        }
    }

}
