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

package org.openvpms.web.component.im.query;

import nextapp.echo2.app.Component;
import org.openvpms.web.echo.focus.FocusGroup;

import java.util.List;


/**
 * Browser for objects matching query criteria.
 *
 * @author Tim Anderson
 */
public interface Browser<T> {

    /**
     * Returns the browser component.
     *
     * @return the browser component
     */
    Component getComponent();

    /**
     * Returns the selected object.
     *
     * @return the selected object, or {@code null} if none has been selected.
     */
    T getSelected();

    /**
     * Select an object.
     *
     * @param object the object to select. May be {@code null} to deselect the current selection
     * @return {@code true} if the object was selected, {@code false} if it doesn't exist in the current view
     */
    boolean setSelected(T object);

    /**
     * Returns the objects matching the query.
     *
     * @return the objects matching the query.
     */
    List<T> getObjects();

    /**
     * Adds a listener to receive notification of selection and query actions.
     *
     * @param listener the listener to add
     */
    void addBrowserListener(BrowserListener<T> listener);

    /**
     * Removes a listener to stop receive notification of selection and query actions.
     *
     * @param listener the listener to remove
     */
    void removeBrowserListener(BrowserListener<T> listener);

    /**
     * Query using the specified criteria, and populate the browser with
     * matches.
     */
    void query();

    /**
     * Returns the browser state.
     * <p/>
     * This may be used to obtain a lightweight representation of the browser's state, in order to restore it later
     * to this object, or another compatible browser.
     *
     * @return the browser state, or <tt>null</tt> if this browser doesn't support externalizing its state
     */
    BrowserState getBrowserState();

    /**
     * Sets the browser state.
     *
     * @param state the state
     */
    void setBrowserState(BrowserState state);

    /**
     * Returns the focus group.
     *
     * @return the focus group
     */
    FocusGroup getFocusGroup();

    /**
     * Sets focus on the results.
     */
    void setFocusOnResults();
}
