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

package org.openvpms.web.workspace.patient.visit;

import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.web.component.app.Context;
import org.openvpms.web.component.im.query.Browser;
import org.openvpms.web.echo.help.HelpContext;
import org.openvpms.web.workspace.customer.CustomerMailContext;
import org.openvpms.web.workspace.patient.history.AbstractPatientHistoryCRUDWindow;
import org.openvpms.web.workspace.patient.history.PatientHistoryBrowser;
import org.openvpms.web.workspace.patient.history.PatientHistoryCRUDWindow;
import org.openvpms.web.workspace.patient.history.PatientHistoryQuery;

import java.util.List;

/**
 * A patient history browser that provides CRUD operations.
 *
 * @author Tim Anderson
 */
public class VisitHistoryBrowserCRUDWindow extends VisitBrowserCRUDWindow<Act> {

    /**
     * The help context.
     */
    private final HelpContext help;

    /**
     * The initial event.
     */
    private Act event;

    /**
     * Constructs a {@link VisitHistoryBrowserCRUDWindow}.
     *
     * @param query   the patient history query
     * @param browser the patient history browser
     * @param context the context
     * @param help    the help context
     */
    public VisitHistoryBrowserCRUDWindow(PatientHistoryQuery query, PatientHistoryBrowser browser, Context context,
                                         HelpContext help) {
        this.help = help;
        if (browser.getSelected() == null) {
            browser.query();
            List<Act> objects = browser.getObjects();
            if (!objects.isEmpty()) {
                browser.setSelected(objects.get(0));
            }
        }
        setBrowser(browser);
        PatientHistoryCRUDWindow window = createWindow(context);
        window.setMailContext(new CustomerMailContext(context, help));
        window.setQuery(query);
        window.setEvent(browser.getSelectedParent());
        setWindow(window);
    }

    /**
     * Sets the event.
     *
     * @param event the event
     */
    public void setEvent(Act event) {
        this.event = event;
    }

    /**
     * Sets the selected object.
     *
     * @param object the selected object
     */
    @Override
    public void setSelected(Act object) {
        super.setSelected(object);
        getWindow().setEvent(getBrowser().getSelectedParent());
    }

    /**
     * Returns the CRUD window.
     *
     * @return the window
     */
    @Override
    public AbstractPatientHistoryCRUDWindow getWindow() {
        return (AbstractPatientHistoryCRUDWindow) super.getWindow();
    }

    /**
     * Returns the browser.
     *
     * @return the browser
     */
    @Override
    public PatientHistoryBrowser getBrowser() {
        return (PatientHistoryBrowser) super.getBrowser();
    }

    /**
     * Invoked when the tab is displayed.
     * <p/>
     * This refreshes the history if the current event being displayed.
     */
    @Override
    public void show() {
        Browser<Act> browser = getBrowser();
        if (browser.getObjects().contains(event)) {
            Act selected = browser.getSelected();
            browser.query();
            if (selected != null) {
                browser.setSelected(selected);
            }
        }
        browser.setFocusOnResults();
    }

    /**
     * Creates a new window.
     *
     * @param context the context
     * @return a new window
     */
    protected PatientHistoryCRUDWindow createWindow(Context context) {
        return new VisitCRUDWindow(context, help);
    }

    /**
     * Selects the current object.
     *
     * @param object the selected object
     */
    @Override
    protected void select(Act object) {
        super.select(object);
        PatientHistoryBrowser browser = getBrowser();
        AbstractPatientHistoryCRUDWindow window = getWindow();
        window.setEvent(browser.getEvent(object));
    }

}
