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

package org.openvpms.web.component.im.edit;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.TextField;
import nextapp.echo2.app.event.WindowPaneEvent;
import nextapp.echo2.app.event.WindowPaneListener;
import org.apache.commons.lang.StringUtils;
import org.openvpms.component.business.domain.im.archetype.descriptor.NodeDescriptor;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.system.common.query.ArchetypeQueryException;
import org.openvpms.web.component.app.Context;
import org.openvpms.web.component.app.LocalContext;
import org.openvpms.web.component.edit.AbstractPropertyEditor;
import org.openvpms.web.component.edit.Property;
import org.openvpms.web.component.edit.Validator;
import org.openvpms.web.component.focus.FocusGroup;
import org.openvpms.web.component.im.layout.DefaultLayoutContext;
import org.openvpms.web.component.im.layout.LayoutContext;
import org.openvpms.web.component.im.query.Query;
import org.openvpms.web.component.im.query.QueryFactory;
import org.openvpms.web.component.im.select.IMObjectSelector;
import org.openvpms.web.component.im.select.IMObjectSelectorListener;
import org.openvpms.web.component.im.util.IMObjectCreator;
import org.openvpms.web.component.im.util.IMObjectCreatorListener;
import org.openvpms.web.component.im.util.IMObjectHelper;


/**
 * Abstract implementation of the {@link IMObjectReferenceEditor} interface.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate$
 */
public abstract class AbstractIMObjectReferenceEditor
        extends AbstractPropertyEditor implements IMObjectReferenceEditor {

    /**
     * The parent object. May be <code>null</code>
     */
    private final IMObject parent;

    /**
     * The selector.
     */
    private IMObjectSelector selector;

    /**
     * Determines if the selector listener is currently being invoked
     * to avoid redundant updates.
     */
    private boolean inListener;

    /**
     * The context.
     */
    private final Context context;


    /**
     * Constructs a new <code>AbstractIMObjectReferenceEditor</code>.
     *
     * @param property the reference property
     * @param parent   the parent object. May be <code>null</code>
     * @param context  the layout context
     */
    public AbstractIMObjectReferenceEditor(Property property,
                                           IMObject parent,
                                           LayoutContext context) {
        this(property, parent, context, false);
    }

    /**
     * Constructs a new <code>AbstractIMObjectReferenceEditor</code>.
     *
     * @param property    the reference property
     * @param parent      the parent object. May be <code>null</code>
     * @param context     the layout context
     * @param allowCreate determines if objects may be created
     */
    public AbstractIMObjectReferenceEditor(Property property,
                                           IMObject parent,
                                           LayoutContext context,
                                           boolean allowCreate) {
        super(property);
        this.parent = parent;
        NodeDescriptor descriptor = property.getDescriptor();
        selector = new IMObjectSelector(descriptor, allowCreate) {
            @Override
            protected Query<IMObject> createQuery(String name) {
                return AbstractIMObjectReferenceEditor.this.createQuery(name);
            }
        };
        selector.setListener(new IMObjectSelectorListener() {
            public void selected(IMObject object) {
                inListener = true;
                try {
                    setObject(object);
                } finally {
                    inListener = false;
                }
            }

            public void create() {
                onCreate();
            }
        });
        IMObjectReference reference = (IMObjectReference) property.getValue();
        if (reference != null) {
            selector.setObject(IMObjectHelper.getObject(reference, descriptor,
                                                        context.getContext()));
        }

        this.context = context.getContext();
    }

    /**
     * Sets the value of the reference to the supplied object.
     *
     * @param object the object. May  be <code>null</code>
     */
    public void setObject(IMObject object) {
        if (!inListener) {
            selector.setObject(object);
        }
        updateProperty(object);
    }

    /**
     * Returns the component.
     *
     * @return the component
     */
    public Component getComponent() {
        return selector.getComponent();
    }

    /**
     * Returns the focus group.
     *
     * @return the focus group
     */
    public FocusGroup getFocusGroup() {
        return selector.getFocusGroup();
    }

    /**
     * Returns the object reference's descriptor.
     *
     * @return the object reference's descriptor
     */
    public NodeDescriptor getDescriptor() {
        return getProperty().getDescriptor();
    }

    /**
     * Determines if the reference is null.
     * This treats an entered but incorrect name as being non-null.
     *
     * @return <code>true</code>  if the reference is null; otherwise
     *         <code>false</code>
     */
    public boolean isNull() {
        boolean result = false;
        if (getProperty().getValue() == null) {
            TextField text = selector.getText();
            if (text == null || StringUtils.isEmpty(text.getText())) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Determines if objects may be created.
     *
     * @param create if <code>true</code>, objects may be created
     */
    public void setAllowCreate(boolean create) {
        selector.setAllowCreate(create);
    }

    /**
     * Determines if objects may be created.
     *
     * @return <code>true</code> if objects may be created
     */
    public boolean allowCreate() {
        return selector.allowCreate();
    }

    /**
     * Validates the object.
     *
     * @param validator the validator
     * @return <code>true</code> if the object and its descendents are valid
     *         otherwise <code>false</code>
     */
    public boolean validate(Validator validator) {
        return (!selector.inSelect()) && super.validate(validator);
    }

    /**
     * Invoked when an object is selected.
     *
     * @param object the selected object
     */
    protected void onSelected(IMObject object) {
        setObject(object);
    }

    /**
     * Invoked to create a new object.
     */
    protected void onCreate() {
        IMObjectCreatorListener listener = new IMObjectCreatorListener() {
            public void created(IMObject object) {
                onCreated(object);
            }

            public void cancelled() {
                // ignore
            }
        };

        IMObjectCreator.create(getDescriptor().getDisplayName(),
                               getDescriptor().getArchetypeRange(),
                               listener);
    }

    /**
     * Invoked when an object is created. Pops up an editor to edit it.
     *
     * @param object the object to edit
     */
    protected void onCreated(IMObject object) {
        Context context = new LocalContext();
        context.setCurrent(object);
        LayoutContext layoutContext = new DefaultLayoutContext(true);
        layoutContext.setContext(context);
        final IMObjectEditor editor
                = IMObjectEditorFactory.create(object, parent, layoutContext);
        final EditDialog dialog = new EditDialog(editor);
        dialog.addWindowPaneListener(new WindowPaneListener() {
            public void windowPaneClosing(WindowPaneEvent event) {
                onEditCompleted(editor);
            }
        });

        dialog.show();
    }

    /**
     * Invoked when the editor is closed.
     *
     * @param editor the editor
     */
    protected void onEditCompleted(IMObjectEditor editor) {
        if (!editor.isCancelled() && !editor.isDeleted()) {
            setObject(editor.getObject());
        }
    }

    /**
     * Creates a query to select objects.
     *
     * @param name a name to filter on. May be <code>null</code>
     * @param name the name to filter on. May be <code>null</code>
     * @return a new query
     * @throws ArchetypeQueryException if the short names don't match any
     *                                 archetypes
     */
    protected Query<IMObject> createQuery(String name) {
        String[] shortNames = getDescriptor().getArchetypeRange();
        Query<IMObject> query = QueryFactory.create(shortNames, context);
        query.setName(name);
        return query;
    }

    /**
     * Updates the underlying property, notifying any registered listeners.
     *
     * @param object the object. May be <code>null</code>
     */
    private void updateProperty(IMObject object) {
        Property property = getProperty();
        if (object != null) {
            property.setValue(object.getObjectReference());
        } else {
            property.setValue(null);
        }
    }

}
