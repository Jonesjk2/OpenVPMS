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

package org.openvpms.web.app.patient.document;

import org.openvpms.component.business.domain.im.act.DocumentAct;
import org.openvpms.component.business.domain.im.archetype.descriptor.ArchetypeDescriptor;
import org.openvpms.component.business.domain.im.archetype.descriptor.NodeDescriptor;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.common.Participation;
import org.openvpms.component.business.domain.im.document.Document;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.system.common.exception.OpenVPMSException;
import org.openvpms.report.DocFormats;
import org.openvpms.report.IMObjectReport;
import org.openvpms.report.IMObjectReportFactory;
import org.openvpms.report.TemplateHelper;
import org.openvpms.web.component.app.Context;
import org.openvpms.web.component.edit.CollectionProperty;
import org.openvpms.web.component.edit.Modifiable;
import org.openvpms.web.component.edit.ModifiableListener;
import org.openvpms.web.component.im.edit.AbstractIMObjectEditor;
import org.openvpms.web.component.im.edit.IMObjectCollectionEditor;
import org.openvpms.web.component.im.edit.SaveHelper;
import org.openvpms.web.component.im.filter.NamedNodeFilter;
import org.openvpms.web.component.im.layout.AbstractLayoutStrategy;
import org.openvpms.web.component.im.layout.IMObjectLayoutStrategy;
import org.openvpms.web.component.im.layout.LayoutContext;
import org.openvpms.web.component.im.util.ErrorHelper;
import org.openvpms.web.component.im.util.IMObjectHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Editor for <em>act.patientDocument</em> acts.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class PatientDocumentActEditor extends AbstractIMObjectEditor {

    /**
     * The last document template.
     */
    private IMObjectReference _lastTemplate;

    /**
     * The document template node.
     */
    private static final String DOC_TEMPLATE = "documentTemplate";

    /**
     * The document reference node.
     */
    private static final String DOC_REFERENCE = "docReference";


    /**
     * Construct a new <code>PatientDocumentActEditor</code>.
     *
     * @param act     the act to edit
     * @param parent  the parent object. May be <code>null</code>
     * @param context the layout context. May be <code>null</code>.
     */
    public PatientDocumentActEditor(DocumentAct act, IMObject parent,
                                    LayoutContext context) {
        super(act, parent, context);
        getEditor(DOC_REFERENCE).addModifiableListener(
                new ModifiableListener() {
                    public void modified(Modifiable modifiable) {
                        onDocumentUpdate();
                    }
                }
        );
        IMObjectCollectionEditor template
                = (IMObjectCollectionEditor) getEditor(DOC_TEMPLATE);
        if (template != null) {
            _lastTemplate = getTemplate();
            template.addModifiableListener(new ModifiableListener() {
                public void modified(Modifiable modifiable) {
                    onTemplateUpdate();
                }
            });
        }
    }

    /**
     * Creates the layout strategy.
     *
     * @return a new layout strategy
     */
    @Override
    protected IMObjectLayoutStrategy createLayoutStrategy() {
        return new LayoutStrategy();
    }

    /**
     * Invoked when the document reference is updated.
     * Invokes {@link #updateFileProperties} with the related document.
     */
    private void onDocumentUpdate() {
        DocumentAct act = (DocumentAct) getObject();
        IMObjectReference docRef = act.getDocReference();
        Document document = (Document) IMObjectHelper.getObject(docRef);
        updateFileProperties(document);
    }

    /**
     * Invoked when the document template updates.
     */
    private void onTemplateUpdate() {
        IMObjectReference template = getTemplate();
        if ((template != null && _lastTemplate != null
                && !template.equals(_lastTemplate))
                || (template != null && _lastTemplate == null)) {
            _lastTemplate = template;
            IMObject patient = Context.getInstance().getPatient();

            DocumentAct act = (DocumentAct) getObject();
            if (patient != null) {
                try {
                    IArchetypeService service
                            = ArchetypeServiceHelper.getArchetypeService();
                    Entity entity = (Entity) IMObjectHelper.getObject(template);
                    if (entity != null) {
                        Document doc = TemplateHelper.getDocumentFromTemplate(
                                entity, service);
                        if (doc != null) {
                            String[] formats = {DocFormats.PDF_TYPE};
                            IMObjectReport report = IMObjectReportFactory.create(
                                    doc, formats, service);
                            Document gen = report.generate(patient);
                            if (SaveHelper.save(gen)) {
                                act.setDocReference(gen.getObjectReference());
                                updateFileProperties(gen);
                            }
                        }
                    }
                } catch (OpenVPMSException exception) {
                    ErrorHelper.show(exception);
                }
            }
        }
    }

    /**
     * Duplicates the document's name and mime-type to the fileName and mimeType
     * properties respectively to the {@link DocumentAct}.
     *
     * @param document the document
     */
    private void updateFileProperties(Document document) {
        String name = null;
        String mimeType = null;
        if (document != null) {
            name = document.getName();
            mimeType = document.getMimeType();
        }
        getProperty("fileName").setValue(name);
        getProperty("mimeType").setValue(mimeType);
    }

    /**
     * Helper to return a reference to the current template, an instance of
     * <em>entity.documentTemplate</em>.
     *
     * @return a reference to the current template. May be <code>null</code>
     */
    private IMObjectReference getTemplate() {
        CollectionProperty property
                = (CollectionProperty) getProperty(DOC_TEMPLATE);
        Collection values = property.getValues();
        if (!values.isEmpty()) {
            Participation p = (Participation) values.toArray()[0];
            return p.getEntity();
        }
        return null;
    }

    /**
     * Layout strategy that treats the 'docReference' node as a simple node.
     */
    private class LayoutStrategy extends AbstractLayoutStrategy {

        /**
         * Returns the 'simple' nodes.
         *
         * @param archetype the archetype
         * @return the simple nodes
         * @see ArchetypeDescriptor#getSimpleNodeDescriptors()
         */
        @Override
        protected List<NodeDescriptor> getSimpleNodes(
                ArchetypeDescriptor archetype) {
            List<NodeDescriptor> nodes = new ArrayList<NodeDescriptor>();
            nodes.addAll(super.getSimpleNodes(archetype));
            boolean found = false;
            for (NodeDescriptor node : nodes) {
                if (node.getName().equals(DOC_REFERENCE)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                nodes.add(archetype.getNodeDescriptor(DOC_REFERENCE));
            }
            return nodes;
        }

        /**
         * Returns the 'complex' nodes.
         *
         * @param archetype the archetype
         * @return the complex nodes
         * @see ArchetypeDescriptor#getComplexNodeDescriptors()
         */
        @Override
        protected List<NodeDescriptor> getComplexNodes(
                ArchetypeDescriptor archetype) {
            return filter(getObject(), super.getComplexNodes(archetype),
                          new NamedNodeFilter(DOC_REFERENCE));
        }

    }

}
