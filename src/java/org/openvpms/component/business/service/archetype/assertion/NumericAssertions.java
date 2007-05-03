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

package org.openvpms.component.business.service.archetype.assertion;

import org.openvpms.component.business.domain.im.archetype.descriptor.ActionTypeDescriptor;
import org.openvpms.component.business.domain.im.archetype.descriptor.AssertionDescriptor;
import org.openvpms.component.business.domain.im.archetype.descriptor.AssertionTypeDescriptor;
import org.openvpms.component.business.domain.im.archetype.descriptor.NodeDescriptor;


/**
 * Assertions for numeric nodes.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 * @see ActionTypeDescriptor
 * @see AssertionTypeDescriptor
 */
public class NumericAssertions {

    /**
     * Determines if a node value is positive (<tt>&gt; 0</tt>).
     *
     * @param value     the node value
     * @param node      the node
     * @param assertion the assertion
     * @return <tt>true</tt> if value is positive, otherwise <tt>false</tt>
     */
    public static boolean positive(Object value, NodeDescriptor node,
                                   AssertionDescriptor assertion) {
        Number number = (Number) value;
        return number.doubleValue() > 0.0;
    }

    /**
     * Determines if a node value is negative (<tt>&lt; 0</tt>).
     *
     * @param value     the node value
     * @param node      the node
     * @param assertion the assertion
     * @return <tt>true</tt> if value is negative, otherwise <tt>false</tt>
     */
    public static boolean negative(Object value, NodeDescriptor node,
                                   AssertionDescriptor assertion) {
        Number number = (Number) value;
        return number.doubleValue() < 0.0;
    }

    /**
     * Determines if a node value is non-negative (<tt>&gt;= 0</tt>).
     *
     * @param value     the node value
     * @param node      the node
     * @param assertion the assertion
     * @return <tt>true</tt> if value is negative, otherwise <tt>false</tt>
     */
    public static boolean nonNegative(Object value, NodeDescriptor node,
                                      AssertionDescriptor assertion) {
        Number number = (Number) value;
        return number.doubleValue() >= 0.0;
    }
}
