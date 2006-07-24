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
 *  Copyright 2005 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.web.app.admin.doc;

import org.openvpms.web.app.subsystem.CRUDWindow;
import org.openvpms.web.app.subsystem.CRUDWorkspace;
import org.openvpms.web.app.subsystem.ShortNameList;
import org.openvpms.web.app.subsystem.ShortNames;


/**
 * Document template workspace.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate$
 */
public class DocumentTemplateWorkspace extends CRUDWorkspace {

    /**
     * Construct a new <code>DocumentTemplateWorkspace</code>.
     */
    public DocumentTemplateWorkspace() {
        super("admin", "documentTemplate", "common", "entity",
              "documentTemplate");
    }

    /**
     * Creates a new CRUD window.
     *
     * @return a new CRUD window
     */
    @Override
    protected CRUDWindow createCRUDWindow() {
        ShortNames shortNames = new ShortNameList(getRefModelName(),
                                                  getEntityName(),
                                                  getConceptName());
        return new DocumentCRUDWindow(getType(), shortNames);
    }

}
