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

package org.openvpms.web.component.im.view;

import nextapp.echo2.app.Component;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.web.component.property.CollectionProperty;


/**
 * Read-only viewer for a collection of {@link IMObject}s.
 *
 * @author Tim Anderson
 */
public interface IMObjectCollectionViewer {

    /**
     * Returns the collection property.
     *
     * @return the collection property
     */
    CollectionProperty getProperty();

    /**
     * Returns the view component.
     *
     * @return the view component
     */
    Component getComponent();

    /**
     * Returns the selected object.
     *
     * @return the selected object. May be {@code null}
     */
    IMObject getSelected();
}
