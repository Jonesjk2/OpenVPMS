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

package org.openvpms.web.app.workflow.messaging;

import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.SplitPane;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.archetype.descriptor.ArchetypeDescriptor;
import org.openvpms.component.business.domain.im.archetype.descriptor.NodeDescriptor;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.lookup.Lookup;
import org.openvpms.component.business.domain.im.security.User;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.DescriptorHelper;
import org.openvpms.component.business.service.archetype.helper.LookupHelper;
import org.openvpms.web.app.subsystem.CRUDWindow;
import org.openvpms.web.app.subsystem.CRUDWindowListener;
import org.openvpms.web.app.subsystem.ShortNameList;
import org.openvpms.web.component.app.GlobalContext;
import org.openvpms.web.component.im.query.ActQuery;
import org.openvpms.web.component.im.query.Browser;
import org.openvpms.web.component.im.query.DefaultActQuery;
import org.openvpms.web.component.im.query.QueryBrowserListener;
import org.openvpms.web.component.im.query.TableBrowser;
import org.openvpms.web.component.subsystem.AbstractWorkspace;
import org.openvpms.web.component.util.ColumnFactory;
import org.openvpms.web.component.util.SplitPaneFactory;

import java.util.List;


/**
 * Messaging workspace.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class MessagingWorkspace extends AbstractWorkspace {

    /**
     * The current user. May be <code>null</code>.
     */
    private User user;

    /**
     * The root component.
     */
    private SplitPane root;

    /**
     * The workspace component.
     */
    private SplitPane workspace;

    /**
     * The CRUD window.
     */
    private CRUDWindow window;

    /**
     * The act browser.
     */
    private Browser<Act> browser;


    /**
     * Construct a new <code>MessagingWorkspace</code>.
     */
    public MessagingWorkspace() {
        super("workflow", "messaging");
        user = GlobalContext.getInstance().getUser();
    }

    /**
     * Determines if the workspace supports an archetype.
     *
     * @param shortName the archetype's short name
     * @return <code>true</code> if the workspace can handle the archetype;
     *         otherwise <code>false</code>
     */
    public boolean canHandle(String shortName) {
        // don't want this workspace participating in context changes, so
        // return false
        return false;
    }

    /**
     * Sets the object to be viewed/edited by the workspace.
     *
     * @param object the object. May be <code>null</code>
     */
    public void setObject(IMObject object) {
        user = (User) object;
        layoutWorkspace(user, root);
    }

    /**
     * Returns the object to to be viewed/edited by the workspace.
     *
     * @return the the object. May be <oode>null</code>
     */
    public IMObject getObject() {
        return user;
    }

    /**
     * Lays out the component.
     *
     * @return the component
     */
    @Override
    protected Component doLayout() {
        root = SplitPaneFactory.create(
                SplitPane.ORIENTATION_VERTICAL,
                "MessagingWorkspace.MainLayout");
        Component heading = super.doLayout();
        root.add(heading);
        if (user != null) {
            layoutWorkspace(user, root);
        }
        return root;
    }

    /**
     * Determines if the workspace should be refreshed. This implementation
     * returns true if the current user has changed.
     *
     * @return <code>true</code> if the workspace should be refreshed, otherwise
     *         <code>false</code>
     */
    @Override
    protected boolean refreshWorkspace() {
        User user = GlobalContext.getInstance().getUser();
        return (user != getObject());
    }

    /**
     * Lays out the workspace.
     *
     * @param user      the user
     * @param container the container
     */
    protected void layoutWorkspace(User user, Component container) {
        ActQuery query = createQuery(user);
        browser = createBrowser(query);
        browser.addQueryListener(new QueryBrowserListener() {
            public void query() {
                selectFirst();
            }

            public void selected(IMObject object) {
                window.setObject(object);
            }
        });

        window = createCRUDWindow();
        window.setListener(new CRUDWindowListener() {
            public void saved(IMObject object, boolean isNew) {
                browser.query();
            }

            public void deleted(IMObject object) {
                browser.query();
            }

            public void refresh(IMObject object) {
                browser.query();
            }
        });
        if (workspace != null) {
            container.remove(workspace);
        }
        workspace = createWorkspace(browser, window);
        container.add(workspace);
        if (!query.isAuto()) {
            browser.query();
        }
    }

    /**
     * Creates the workspace.
     *
     * @param browser the act browser
     * @param window  the CRUD window
     * @return a new split pane representing the workspace
     */
    private SplitPane createWorkspace(Browser<Act> browser, CRUDWindow window) {
        Column acts = ColumnFactory.create("Inset", browser.getComponent());
        return SplitPaneFactory.create(SplitPane.ORIENTATION_VERTICAL,
                                       "MessagingWorkspace.Layout", acts,
                                       window.getComponent());
    }

    /**
     * Creates the CRUD window.
     *
     * @return a new CRUD window
     */
    private CRUDWindow createCRUDWindow() {
        return new MessagingCRUDWindow(
                DescriptorHelper.getDisplayName("act.userMessage"),
                new ShortNameList("act.userMessage"));
    }

    /**
     * Creates the act browser.
     *
     * @param query the act query
     * @return a new act browser
     */
    private TableBrowser<Act> createBrowser(ActQuery query) {
        return new TableBrowser<Act>(query, null);
    }

    /**
     * Creates a new query.
     *
     * @param user the user to query
     * @return a new query
     */
    private ActQuery createQuery(User user) {
        String shortName = "act.userMessage";
        String[] shortNames = {shortName};
        IArchetypeService service
                = ArchetypeServiceHelper.getArchetypeService();
        ArchetypeDescriptor archetype
                = DescriptorHelper.getArchetypeDescriptor(shortName);
        NodeDescriptor statuses = archetype.getNodeDescriptor("status");
        List<Lookup> lookups = LookupHelper.get(service, statuses);
        return new DefaultActQuery(user, "to",
                                   "participation.user",
                                   shortNames, lookups, null);
    }

    /**
     * Selects the first available message.
     */
    private void selectFirst() {
        List<Act> objects = browser.getObjects();
        if (!objects.isEmpty()) {
            Act current = objects.get(0);
            browser.setSelected(current);
            window.setObject(current);
        } else {
            window.setObject(null);
        }
    }

}
