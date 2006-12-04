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

package org.openvpms.web.component.im.query;

import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.system.common.query.SortConstraint;
import org.openvpms.web.component.im.layout.DefaultLayoutContext;
import org.openvpms.web.component.im.layout.LayoutContext;
import org.openvpms.web.component.im.table.IMObjectTableModelFactory;
import org.openvpms.web.component.im.table.IMTableModel;
import org.openvpms.web.component.im.view.IMObjectComponentFactory;
import org.openvpms.web.component.im.view.TableComponentFactory;

/**
 * Add description here.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class IMObjectTableBrowser<T extends IMObject> extends TableBrowser<T> {

    /**
     * Construct a new <code>IMObjectTableBrowser</code> that queries IMObjects
     * using the specified query.
     *
     * @param query the query
     */
    public IMObjectTableBrowser(Query<T> query) {
        this(query, null);
    }

    /**
     * Construct a new <code>IMObjectTableBrowser</code> that queries IMObjects
     * using the specified query, displaying them in the table.
     *
     * @param query the query
     * @param sort  the sort criteria. May be <code>null</code>
     */
    public IMObjectTableBrowser(Query<T> query, SortConstraint[] sort) {
        super(query, sort, createTableModel(query));
    }

    /**
     * Construct a new <code>Browser</code> that queries IMObjects using the
     * specified query, displaying them in the table.
     *
     * @param query the query
     * @param sort  the sort criteria. May be <code>null</code>
     * @param model the table model
     */
    public IMObjectTableBrowser(Query<T> query, SortConstraint[] sort,
                                IMTableModel<T> model) {
        super(query, sort, model);
    }

    /**
     * Creates a new table model for the specified query.
     *
     * @param query the query
     * @return a new table model
     */
    private static <T extends IMObject> IMTableModel<T> createTableModel(
            Query<T> query) {
        LayoutContext context = new DefaultLayoutContext();
        IMObjectComponentFactory factory = new TableComponentFactory(context);
        context.setComponentFactory(factory);
        return IMObjectTableModelFactory.create(query.getShortNames(),
                                                context);
    }

}
