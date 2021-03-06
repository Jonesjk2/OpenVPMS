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

package org.openvpms.web.component.im.doc;

import org.openvpms.component.business.domain.im.act.DocumentAct;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.web.component.im.layout.ArchetypeNodes;
import org.openvpms.web.component.im.layout.LayoutContext;
import org.openvpms.web.component.im.view.ComponentState;
import org.openvpms.web.component.property.Property;


/**
 * A layout strategy for {@link DocumentAct}s that enables the document to be viewed.
 *
 * @author Tim Anderson
 */
public class DocumentActViewLayoutStrategy extends DocumentActLayoutStrategy {

    /**
     * Creates a component for a property.
     *
     * @param property the property
     * @param parent   the parent object
     * @param context  the layout context
     * @return a component to display {@code property}
     */
    @Override
    protected ComponentState createComponent(Property property, IMObject parent, LayoutContext context) {
        String name = property.getName();
        ComponentState result;
        if (name.equals("documentTemplate")) {
            boolean template = hasDocumentNode(parent);
            DocumentViewer viewer = new DocumentViewer((DocumentAct) parent, true, template, context);
            result = new ComponentState(viewer.getComponent(), property);
        } else if (name.equals(DOCUMENT)) {
            DocumentViewer viewer = new DocumentViewer((DocumentAct) parent, true, false, context);
            result = new ComponentState(viewer.getComponent(), property);
        } else {
            result = super.createComponent(property, parent, context);
        }
        return result;
    }

    /**
     * Returns {@link ArchetypeNodes} to determine which nodes will be displayed.
     *
     * @return the archetype nodes
     */
    @Override
    protected ArchetypeNodes getArchetypeNodes() {
        return VIEW_NODES;
    }

    /**
     * Determines if an object has a document node.
     *
     * @param object the object to check
     * @return {@code true} if the object has a document node, otherwise {@code false}
     */
    private boolean hasDocumentNode(IMObject object) {
        IMObjectBean bean = new IMObjectBean(object);
        return bean.hasNode(DOCUMENT);
    }

}

