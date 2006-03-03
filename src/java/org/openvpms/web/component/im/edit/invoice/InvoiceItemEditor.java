package org.openvpms.web.component.im.edit.invoice;

import org.openvpms.component.business.domain.im.archetype.descriptor.ArchetypeDescriptor;
import org.openvpms.component.business.domain.im.archetype.descriptor.NodeDescriptor;
import org.openvpms.component.business.domain.im.common.Act;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.common.Participation;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.domain.im.product.ProductPrice;
import org.openvpms.web.component.edit.Property;
import org.openvpms.web.component.im.edit.IMObjectEditor;
import org.openvpms.web.component.im.edit.act.ActItemEditor;
import org.openvpms.web.component.im.util.DescriptorHelper;
import org.openvpms.web.component.im.util.IMObjectHelper;


/**
 * An editor for {@link Act}s which have an archetype of
 * <em>act.customerInvoice</em>.
 *
 * @author <a href="mailto:tma@netspace.net.au">Tim Anderson</a>
 * @version $LastChangedDate:2006-02-21 03:48:29Z $
 */
public class InvoiceItemEditor extends ActItemEditor {

    /**
     * Construct a new <code>EstimationItemEdtor</code>.
     *
     * @param act        the act to edit
     * @param parent     the parent object. May be <code>null</code>
     * @param descriptor the parent descriptor. May be <code>null</cocde>
     * @param showAll    if <code>true</code> show optional and required fields;
     *                   otherwise show required fields.
     */
    protected InvoiceItemEditor(Act act, IMObject parent,
                                NodeDescriptor descriptor, boolean showAll) {
        super(act, parent, descriptor, showAll);
    }

    /**
     * Create a new editor for an object, if it can be edited by this class.
     *
     * @param object     the object to edit
     * @param parent     the parent object. May be <code>null</code>
     * @param descriptor the parent descriptor. May be <code>null</cocde>
     * @param showAll    if <code>true</code> show optional and required fields;
     *                   otherwise show required fields.
     * @return a new editor for <code>object</code>, or <code>null</code> if it
     *         cannot be edited by this
     */
    public static IMObjectEditor create(IMObject object, IMObject parent,
                                        NodeDescriptor descriptor,
                                        boolean showAll) {
        IMObjectEditor result = null;
        if (object instanceof Act) {
            ArchetypeDescriptor archetype
                    = DescriptorHelper.getArchetypeDescriptor(object);
            if (archetype != null
                && archetype.getShortName().equals("act.customerInvoiceItem")) {
                result = new InvoiceItemEditor((Act) object, parent,
                                               descriptor, showAll);
            }
        }
        return result;
    }

    /**
     * Invoked when the participation product is changed, to update prices.
     *
     * @param participation the product participation instance
     */
    protected void productModified(Participation participation) {
        IMObjectReference entity = participation.getEntity();
        IMObject object = IMObjectHelper.getObject(entity);
        if (object instanceof Product) {
            Property fixedPrice = getProperty("fixedPrice");
            Property unitPrice = getProperty("unitPrice");
            Product product = (Product) object;
            ProductPrice fixed = getPrice("productPrice.fixedPrice", product);
            ProductPrice unit = getPrice("productPrice.unitPrice", product);
            if (fixed != null) {
                fixedPrice.setValue(fixed.getPrice());
            }
            if (unit != null) {
                unitPrice.setValue(unit.getPrice());
            }
        }
    }


}
