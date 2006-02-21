package org.openvpms.web.component.edit;

import java.util.List;

import nextapp.echo2.app.Component;
import nextapp.echo2.app.Label;
import nextapp.echo2.app.SelectField;
import nextapp.echo2.app.list.ListModel;
import org.apache.commons.jxpath.Pointer;

import org.openvpms.component.business.domain.im.archetype.descriptor.NodeDescriptor;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.lookup.ILookupService;
import org.openvpms.web.component.LabelFactory;
import org.openvpms.web.component.SelectFieldFactory;
import org.openvpms.web.component.bound.BoundDateField;
import org.openvpms.web.component.bound.BoundPalette;
import org.openvpms.web.component.im.AbstractIMObjectComponentFactory;
import org.openvpms.web.component.list.IMObjectListCellRenderer;
import org.openvpms.web.component.list.LookupListCellRenderer;
import org.openvpms.web.component.list.LookupListModel;
import org.openvpms.web.component.palette.Palette;
import org.openvpms.web.component.validator.NodeValidator;
import org.openvpms.web.component.validator.ValidatingPointer;
import org.openvpms.web.spring.ServiceHelper;


/**
 * Factory for editors for {@link IMObject} instances.
 *
 * @author <a href="mailto:tma@netspace.net.au">Tim Anderson</a>
 * @version $LastChangedDate$
 */
public class NodeEditorFactory extends AbstractIMObjectComponentFactory {

    /**
     * The modification tracker;
     */
    private ModifiableSet _modifiable;

    /**
     * The lookup service.
     */
    private ILookupService _lookup;

    /**
     * Construct a new <code>NodeEditorFactory</code>.
     *
     * @param modifiable the modification tracker
     */
    public NodeEditorFactory(ModifiableSet modifiable) {
        _modifiable = modifiable;
    }

    /**
     * Create a component to display an object.
     *
     * @param context    the context object
     * @param descriptor the object's descriptor
     * @return a component to display <code>object</code>
     */
    public Component create(IMObject context, NodeDescriptor descriptor) {
        Component result;
        if (descriptor.isLookup()) {
            result = getSelectEditor(context, descriptor);
        } else if (descriptor.isBoolean()) {
            result = getCheckBox(context, descriptor);
        } else if (descriptor.isString()) {
            result = getTextComponent(context, descriptor);
        } else if (descriptor.isNumeric()) {
            result = getNumericComponent(context, descriptor);
        } else if (descriptor.isDate()) {
            result = getDateEditor(context, descriptor);
        } else if (descriptor.isCollection()) {
            result = getCollectionEditor(context, descriptor);
        } else if (descriptor.isObjectReference()) {
            result = getObjectReferenceEditor(context, descriptor);
        } else {
            Label label = LabelFactory.create();
            label.setText("No editor for type " + descriptor.getType());
            result = label;
        }
        if (descriptor.isReadOnly()) {
            result.setEnabled(false);
        }
        return result;
    }

    private Component getNumericComponent(IMObject context, NodeDescriptor descriptor) {
        int maxColumns = 10;
        return getTextComponent(context, descriptor, maxColumns);
    }

    /**
     * Create a component to display an object.
     *
     * @param object     the object to display
     * @param context    the object's parent. May be <code>null</code>
     * @param descriptor the parent object's descriptor. May be
     *                   <code>null</code>
     */
    public Component create(IMObject object, IMObject context,
                            NodeDescriptor descriptor) {
        IMObjectEditor editor = IMObjectEditorFactory.create(object, context,
                descriptor, true);
        _modifiable.add(object, editor);
        return editor.getComponent();
    }

    /**
     * Returns a component to edit a date node.
     *
     * @param object     the parent object
     * @param descriptor the node descriptor
     * @return a component to edit the node
     */
    protected Component getDateEditor(IMObject object,
                                      NodeDescriptor descriptor) {
        Pointer pointer = getPointer(object, descriptor);
        return new BoundDateField(pointer);
    }

    /**
     * Returns a component to edit a lookup node.
     *
     * @param object     the parent object
     * @param descriptor the node descriptor
     * @return a component to edit the node
     */
    protected Component getSelectEditor(IMObject object,
                                        NodeDescriptor descriptor) {
        Pointer pointer = getPointer(object, descriptor);
        ListModel model = new LookupListModel(object, descriptor,
                getLookupService());
        SelectField field = SelectFieldFactory.create(pointer, model);
        field.setCellRenderer(new LookupListCellRenderer());
        return field;
    }

    /**
     * Returns a component to edit a collection node.
     *
     * @param object     the parent object
     * @param descriptor the node descriptor
     * @return a component to edit the node
     */
    protected Component getCollectionEditor(IMObject object, NodeDescriptor descriptor) {
        Component result;
        if (descriptor.isParentChild()) {
            CollectionEditor editor = new CollectionEditor(object, descriptor);
            _modifiable.add(object, editor);
            result = editor.getComponent();
        } else {
            List<IMObject> identifiers = ArchetypeServiceHelper.getCandidateChildren(
                    ServiceHelper.getArchetypeService(),
                    descriptor, object);
            Pointer pointer = getPointer(object, descriptor);
            Palette palette = new BoundPalette(identifiers, pointer);
            palette.setCellRenderer(new IMObjectListCellRenderer());
            result = palette;
        }
        return result;
    }

    /**
     * Returns a component to edit an object reference.
     *
     * @param object     the parent object
     * @param descriptor the node descriptor
     * @return a component to edit the node
     */
    protected Component getObjectReferenceEditor(IMObject object,
                                                 NodeDescriptor descriptor) {
        Pointer pointer = getPointer(object, descriptor);
        ObjectReferenceEditor editor
                = new ObjectReferenceEditor(pointer, descriptor);
        return editor.getComponent();
    }

    /**
     * Helper to return a pointer to an attribute given its descriptor. This
     * implementation returns a {@link ValidatingPointer}.
     *
     * @param object     the object that owne the attribute
     * @param descriptor the attribute's descriptor
     * @return a pointer to the attribute identified by <code>descriptor</code>.
     */
    @Override
    protected Pointer getPointer(IMObject object, NodeDescriptor descriptor) {
        Pointer pointer = super.getPointer(object, descriptor);
        NodeValidator validator = new NodeValidator(descriptor);
        ValidatingPointer result = new ValidatingPointer(pointer, validator);
        _modifiable.add(object, result);
        return result;
    }

    /**
     * Helper to return the lookup service.
     *
     * @return the lookup service
     */
    private ILookupService getLookupService() {
        if (_lookup == null) {
            _lookup = ServiceHelper.getLookupService();
        }
        return _lookup;
    }

}
