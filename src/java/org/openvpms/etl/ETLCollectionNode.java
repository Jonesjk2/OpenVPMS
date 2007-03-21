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

package org.openvpms.etl;

import java.util.Set;
import java.util.HashSet;

/**
 * Add description here.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class ETLCollectionNode extends ETLNode {

    private Set<ETLReference> values;

    public ETLCollectionNode() {

    }

    public ETLCollectionNode(String name) {
        super(name);
    }

    public Set<ETLReference> getValues() {
        return values;
    }

    public void addValue(ETLReference value) {
        if (values == null) {
            values = new HashSet<ETLReference>();
        }
        values.add(value);
    }

    protected void setValues(Set<ETLReference> values) {
        this.values = values;
    }
}
