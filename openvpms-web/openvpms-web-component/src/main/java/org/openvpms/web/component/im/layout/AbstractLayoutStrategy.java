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

package org.openvpms.web.component.im.layout;

import nextapp.echo2.app.Column;
import nextapp.echo2.app.Component;
import nextapp.echo2.app.Grid;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.SelectField;
import org.openvpms.component.business.domain.im.archetype.descriptor.ArchetypeDescriptor;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.service.archetype.helper.DescriptorHelper;
import org.openvpms.web.component.im.filter.NodeFilter;
import org.openvpms.web.component.im.view.ComponentState;
import org.openvpms.web.component.im.view.IMObjectComponentFactory;
import org.openvpms.web.component.property.Property;
import org.openvpms.web.component.property.PropertySet;
import org.openvpms.web.component.property.ReadOnlyProperty;
import org.openvpms.web.echo.factory.ColumnFactory;
import org.openvpms.web.echo.factory.LabelFactory;
import org.openvpms.web.echo.factory.RowFactory;
import org.openvpms.web.echo.focus.FocusGroup;
import org.openvpms.web.echo.style.Styles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openvpms.web.echo.style.Styles.CELL_SPACING;


/**
 * Abstract implementation of the {@link IMObjectLayoutStrategy} interface.
 *
 * @author Tim Anderson
 */
public abstract class AbstractLayoutStrategy implements IMObjectLayoutStrategy {

    /**
     * Default nodes to render.
     */
    public static ArchetypeNodes DEFAULT_NODES = new ArchetypeNodes();


    /**
     * The component states, used to determine initial focus.
     */
    private ComponentSet components;

    /**
     * Pre-created component states, keyed on property name.
     */
    private Map<String, ComponentState> states = new HashMap<String, ComponentState>();

    /**
     * The focus group of the current component.
     */
    private FocusGroup focusGroup;

    /**
     * If {@code true} keep layout state after invoking {@code apply()}. Use this if the same strategy will be used
     * to layout a component multiple times.
     */
    private boolean keepState;

    /**
     * Sanity checker to detect recursion.
     */
    private boolean inApply;


    /**
     * Constructs an {@link AbstractLayoutStrategy}.
     */
    public AbstractLayoutStrategy() {
        this(false);
    }

    /**
     * Constructs an {@link AbstractLayoutStrategy}.
     *
     * @param keepState if {@code true} keep layout state. Use this if the same strategy will be used to layout a
     *                  component multiple times
     */
    public AbstractLayoutStrategy(boolean keepState) {
        this.keepState = keepState;
    }

    /**
     * Pre-registers a component for inclusion in the layout.
     * <p/>
     * The component must be associated with a property.
     *
     * @param state the component state
     * @throws IllegalStateException if the component isn't associated with a property
     */
    public void addComponent(ComponentState state) {
        Property property = state.getProperty();
        if (property == null) {
            throw new IllegalArgumentException("Argument 'state' must be associated with a property");
        }
        states.put(property.getName(), state);
    }

    /**
     * Apply the layout strategy.
     * <p/>
     * This renders an object in a {@code Component}, using a factory to create the child components.
     *
     * @param object     the object to apply
     * @param properties the object's properties
     * @param parent     the parent object. May be {@code null}
     * @param context    the layout context
     * @return the component containing the rendered {@code object}
     */
    public ComponentState apply(IMObject object, PropertySet properties, IMObject parent, LayoutContext context) {
        ComponentState state;
        if (inApply) {
            throw new IllegalStateException("Cannot call apply() recursively");
        }
        inApply = true;
        try {
            focusGroup = new FocusGroup(DescriptorHelper.getDisplayName(object));
            components = new ComponentSet(focusGroup);
            Component container = doLayout(object, properties, parent, context);
            focusGroup.setDefault(getDefaultFocus());
            state = new ComponentState(container, focusGroup);
            components = null;
            if (!keepState) {
                focusGroup = null;
            }
        } finally {
            inApply = false;
        }
        return state;
    }

    /**
     * Returns the focus group.
     *
     * @return the focus group, or {@code null} if it hasn't been initialised
     */
    protected FocusGroup getFocusGroup() {
        return focusGroup;
    }

    /**
     * Lay out out the object.
     *
     * @param object     the object to lay out
     * @param properties the object's properties
     * @param parent     the parent object. May be {@code null}
     * @param context    the layout context
     * @return the component
     */
    protected Component doLayout(IMObject object, PropertySet properties, IMObject parent, LayoutContext context) {
        Column container = ColumnFactory.create(CELL_SPACING);
        doLayout(object, properties, parent, container, context);
        return container;
    }

    /**
     * Lay out out the object in the specified container.
     *
     * @param object     the object to lay out
     * @param properties the object's properties
     * @param parent     the parent object. May be {@code null}
     * @param container  the container to use
     * @param context    the layout context
     */
    protected void doLayout(IMObject object, PropertySet properties, IMObject parent, Component container,
                            LayoutContext context) {
        ArchetypeDescriptor archetype = context.getArchetypeDescriptor(object);
        ArchetypeNodes nodes = getArchetypeNodes(object, context);
        NodeFilter filter = getNodeFilter(object, context);

        List<Property> simple = nodes.getSimpleNodes(properties, archetype, object, filter);
        List<Property> complex = nodes.getComplexNodes(properties, archetype, object, filter);

        doSimpleLayout(object, parent, simple, container, context);
        doComplexLayout(object, parent, complex, container, context);
    }

    /**
     * Lays out child components in a grid.
     *
     * @param object     the object to lay out
     * @param parent     the parent object. May be {@code null}
     * @param properties the properties
     * @param container  the container to use
     * @param context    the layout context
     */
    protected void doSimpleLayout(IMObject object, IMObject parent, List<Property> properties,
                                  Component container, LayoutContext context) {
        if (!properties.isEmpty()) {
            ComponentGrid grid = createGrid(object, properties, context);
            Component component = createGrid(grid);
            container.add(ColumnFactory.create(Styles.INSET, component));
        }
    }

    /**
     * Lays out components in a grid.
     *
     * @param object     the object to lay out
     * @param properties the properties
     * @param context    the layout context
     */
    protected ComponentGrid createGrid(IMObject object, List<Property> properties,
                                       LayoutContext context) {
        int columns = getColumns(properties);
        return createGrid(object, properties, context, columns);
    }

    /**
     * Lays out components in a grid.
     *
     * @param object     the object to lay out
     * @param properties the properties
     * @param context    the layout context
     */
    protected ComponentGrid createGrid(IMObject object, List<Property> properties, LayoutContext context, int columns) {
        ComponentSet set = createComponentSet(object, properties, context);
        ComponentGrid grid = new ComponentGrid();
        grid.add(set, columns);
        return grid;
    }

    /**
     * Creates a grid.
     *
     * @param grid the component grid
     * @return the corresponding grid
     */
    protected Grid createGrid(ComponentGrid grid) {
        return grid.createGrid(components);
    }

    /**
     * Determines the no. of columns to display.
     *
     * @param properties the node descriptors
     * @return the number of columns
     */
    protected int getColumns(List<Property> properties) {
        return (properties.size() <= 4) ? 1 : 2;
    }

    /**
     * Lays out each child component in a tabbed pane.
     *
     * @param object     the object to lay out
     * @param parent     the parent object. May be {@code null}
     * @param properties the properties
     * @param container  the container to use
     * @param context    the layout context
     */
    protected void doComplexLayout(IMObject object, IMObject parent, List<Property> properties, Component container,
                                   LayoutContext context) {
        if (!properties.isEmpty()) {
            IMObjectTabPaneModel model = doTabLayout(object, properties, container, context, false);
            IMObjectTabPane pane = new IMObjectTabPane(model);

            pane.setSelectedIndex(0);
            container.add(pane);
        }
    }

    /**
     * Returns {@link ArchetypeNodes} to determine which nodes will be displayed.
     *
     * @param object  the object to display
     * @param context the layout context
     * @return the archetype nodes
     */
    protected ArchetypeNodes getArchetypeNodes(IMObject object, LayoutContext context) {
        return getArchetypeNodes();
    }

    /**
     * Returns {@link ArchetypeNodes} to determine which nodes will be displayed.
     *
     * @return the archetype nodes
     */
    protected ArchetypeNodes getArchetypeNodes() {
        return DEFAULT_NODES;
    }

    /**
     * Returns a node filter to filter nodes. This implementation returns {@link LayoutContext#getDefaultNodeFilter()}.
     *
     * @param object  the object to filter nodes for
     * @param context the context
     * @return a node filter to filter nodes, or {@code null} if no filtering is required
     */
    protected NodeFilter getNodeFilter(IMObject object, LayoutContext context) {
        return context.getDefaultNodeFilter();
    }

    /**
     * Lays out a component grid.
     *
     * @param grid      the grid
     * @param container the container to add the grid to
     */
    protected void doGridLayout(ComponentGrid grid, Component container) {
        Grid g = grid.createGrid(components);
        container.add(g);
    }

    /**
     * Lays out child components in a tab model.
     *
     * @param object       the parent object
     * @param properties   the properties
     * @param container    the container
     * @param context      the layout context
     * @param shortcutHint a hint to display short cuts for tabs. If {@code false} shortcuts will only be displayed if
     *                     there is more than one descriptor. Shortcuts will never be displayed if the layout depth
     *                     is non-zero
     * @return the tab model
     */
    protected IMObjectTabPaneModel doTabLayout(IMObject object, List<Property> properties, Component container,
                                               LayoutContext context, boolean shortcutHint) {
        IMObjectTabPaneModel model;
        boolean shortcuts = false;
        if (context.getLayoutDepth() == 0 && (properties.size() > 1 || shortcutHint)) {
            model = createTabModel(container);
            shortcuts = true;
        } else {
            model = createTabModel(null);
        }
        doTabLayout(object, properties, model, context, shortcuts);
        return model;
    }

    /**
     * Creates a new tab model.
     *
     * @param container the tab container. May be {@code null}
     * @return a new tab model
     */
    protected IMObjectTabPaneModel createTabModel(Component container) {
        return new IMObjectTabPaneModel(container);
    }

    /**
     * Lays out child components in a tab model.
     *
     * @param object     the parent object
     * @param properties the properties
     * @param model      the tab model
     * @param context    the layout context
     * @param shortcuts  if {@code true} include short cuts
     */
    protected void doTabLayout(IMObject object, List<Property> properties, IMObjectTabPaneModel model,
                               LayoutContext context, boolean shortcuts) {
        for (Property property : properties) {
            ComponentState child = createComponent(property, object, context);
            addTab(model, property, child, shortcuts);
        }
    }

    /**
     * Adds a tab to a tab model.
     *
     * @param model       the tab  model
     * @param property    property
     * @param component   the component to add
     * @param addShortcut if {@code true} add a tab shortcut
     */
    protected void addTab(IMObjectTabPaneModel model, Property property, ComponentState component,
                          boolean addShortcut) {
        setFocusTraversal(component);
        String text = component.getDisplayName();
        if (text == null) {
            text = property.getDisplayName();
        }
        if (addShortcut && model.size() < 10) {
            text = getShortcut(text, model.size() + 1);
        }
        Component child = component.getComponent();
        if (child instanceof SelectField) {
            // workaround for render bug in firefox. See OVPMS-239
            child = RowFactory.create(child);
        }

        if (LayoutHelper.needsInset(child)) {
            child = ColumnFactory.create(Styles.INSET, child);
        }
        model.addTab(property, text, child);
    }

    /**
     * Creates a set of components to be rendered from the supplied descriptors.
     *
     * @param object     the parent object
     * @param properties the properties
     * @param context    the layout context
     * @return the components
     */
    protected ComponentSet createComponentSet(IMObject object, List<Property> properties,
                                              LayoutContext context) {
        ComponentSet result = new ComponentSet();
        for (Property property : properties) {
            ComponentState component = createComponent(property, object, context);
            result.add(component);
        }
        return result;
    }

    /**
     * Helper to add a node to a grid.
     *
     * @param grid      the grid
     * @param name      the node display name. May be {@code null}
     * @param component the component representing the node
     */
    protected void add(ComponentGrid grid, String name, Component component) {
        Label label = LabelFactory.create();
        if (name != null) {
            label.setText(name);
        }
        grid.add(label, component);
    }

    /**
     * Helper to add a node to a container.
     *
     * @param container the container
     * @param name      the node display name. May be {@code null}
     * @param component the component representing the node
     */
    protected void add(Component container, String name, Component component) {
        Label label = LabelFactory.create();
        if (name != null) {
            label.setText(name);
        }
        add(container, label, component);
    }

    /**
     * Helper to add a node to a container.
     *
     * @param container the container
     * @param label     the component label
     * @param component the component representing the node
     */
    protected void add(Component container, Label label, Component component) {
        container.add(label);
        container.add(component);
    }

    /**
     * Helper to add a node to a container, setting its tab index.
     *
     * @param container the container
     * @param name      the node display name
     * @param component the component representing the node
     */
    protected void add(Component container, String name, ComponentState component) {
        add(container, name, component.getComponent());
        setFocusTraversal(component);
    }

    /**
     * Helper to add a property to a container, setting its tab index.
     *
     * @param container the container
     * @param component the component representing the property
     */
    protected void add(Component container, ComponentState component) {
        Property property = component.getProperty();
        String name = null;
        if (property != null) {
            name = property.getDisplayName();
        }
        add(container, name, component);
        setFocusTraversal(component);
    }

    /**
     * Creates a component for a property.
     * <p/>
     * If there is a pre-existing component, registered via {@link #addComponent}, this will be returned.
     *
     * @param property the property
     * @param parent   the parent object
     * @param context  the layout context
     * @return a component to display {@code property}
     */
    protected ComponentState createComponent(Property property, IMObject parent, LayoutContext context) {
        ComponentState result = getComponent(property);
        if (result == null) {
            IMObjectComponentFactory factory = context.getComponentFactory();
            result = factory.create(property, parent);
        }
        return result;
    }

    /**
     * Returns the component associated with the specified property.
     *
     * @param property the property
     * @return the corresponding component, or {@code null} if none is found
     */
    protected ComponentState getComponent(Property property) {
        return getComponent(property.getName());
    }

    /**
     * Returns the component associated with the specified property.
     *
     * @param name the property name
     * @return the corresponding component, or {@code null} if none is found
     */
    protected ComponentState getComponent(String name) {
        return states.get(name);
    }

    /**
     * Returns the default focus component.
     * <p/>
     * Delegates to {@link #getDefaultFocus(ComponentSet)}.
     *
     * @return the default focus component, or {@code null} if none is found
     */
    protected Component getDefaultFocus() {
        return getDefaultFocus(components);
    }

    /**
     * Returns the default focus component.
     * <p/>
     * This implementation returns the first focusable component.
     *
     * @param components the components
     * @return the default focus component, or {@code null} if none is found
     */
    protected Component getDefaultFocus(ComponentSet components) {
        return components.getFocusable();
    }

    /**
     * Sets the focus traversal index of a component, if it is a focus traversal participant.
     * NOTE: if a component doesn't specify a focus group, this may register a child component with the focus group
     * rather than the parent.
     *
     * @param state the component state
     */
    protected void setFocusTraversal(ComponentState state) {
        if (components != null) {    // will be null if apply() has completed
            components.setFocusTraversal(state);
        }
    }

    /**
     * Returns a shortcut for a tab.
     * Shortcuts no.s must be from 1..10, and will be displayed as '1..9, 0'.
     *
     * @param name     the tab name
     * @param shortcut the shortcut no.
     * @return the shortcut text
     */
    protected String getShortcut(String name, int shortcut) {
        if (shortcut == 10) {
            shortcut = 0;
        }
        return "&" + shortcut + " " + name;
    }

    /**
     * Creates a read-only version of the supplied property.
     *
     * @param property the property
     * @return a read-only version of the property
     */
    protected Property createReadOnly(Property property) {
        return new ReadOnlyProperty(property);
    }

}

