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

package org.openvpms.web.component.bound;

import nextapp.echo2.app.event.ActionEvent;
import nextapp.echo2.app.event.ActionListener;
import org.openvpms.web.component.edit.Property;
import org.openvpms.web.component.util.DateFieldImpl;
import org.openvpms.web.component.util.DateHelper;

import java.util.Date;


/**
 * Binds a {@link Property} to a <code>DateField</code>.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate$
 */
public class BoundDateField extends DateFieldImpl {

    /**
     * The bound property.
     */
    private final DateBinder binder;

    /**
     * If <tt>true</tt>, include the current time if the date is today.
     */
    private boolean includeTimeForToday = true;


    /**
     * Construct a new <code>BoundDateField</code>.
     *
     * @param property the property to bind
     */
    public BoundDateField(Property property) {
        // Register an action listener to ensure document update events
        // are triggered in a timely fashion
        getTextField().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                // no-op.
            }
        });

        binder = createBinder(property);
        binder.setField();
    }

    /**
     * Includes the current time if the selected date is today.
     * For all other days, the time is set to <tt>0:0:0</tt>.
     * Defaults to <tt>true</tt>.
     *
     * @param include if <tt>true</tt>, include the current time if the date is
     *                today; otherwise set it to <tt>0:0:0</tt>
     */
    public void setIncludeTimeForToday(boolean include) {
        includeTimeForToday = include;
    }

    /**
     * Creates a new {@link DateBinder}.
     *
     * @param property the property to bind
     * @return a new binder
     */
    protected DateBinder createBinder(Property property) {
        return new DateBinder(this, property) {
            @Override
            protected Object getFieldValue() {
                Date date = getField().getSelectedDate().getTime();
                if (includeTimeForToday) {
                    return DateHelper.getDatetimeIfToday(date);
                }
                return date;
            }
        };
    }

    /**
     * Returns the binder.
     *
     * @return the binder
     */
    protected DateBinder getBinder() {
        return binder;
    }

}
