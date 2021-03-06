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

package org.openvpms.web.component.im.doc;

import org.openvpms.archetype.rules.doc.TemplateHelper;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.Participation;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.web.component.im.edit.AbstractIMObjectEditor;
import org.openvpms.web.component.im.edit.IMObjectEditor;
import org.openvpms.web.component.im.edit.IMObjectEditorFactory;
import org.openvpms.web.component.im.layout.IMObjectLayoutStrategy;
import org.openvpms.web.component.im.layout.LayoutContext;
import org.openvpms.web.component.im.util.IMObjectCreator;
import org.openvpms.web.component.property.Modifiable;
import org.openvpms.web.component.property.ModifiableListener;
import org.openvpms.web.system.ServiceHelper;


/**
 * Editor for <em>entity.documentTemplate</em>s.
 * This archetype has an implicit participation of type
 * <em>participation.document</em> which cannot be represented using archetypes,
 * as participations are typically navigated from an Act, not an Entity.
 *
 * @author Tim Anderson
 */
public class DocumentTemplateEditor extends AbstractIMObjectEditor {

    /**
     * The participation editor.
     */
    private IMObjectEditor participationEditor;


    /**
     * Constructs a {@link DocumentTemplateEditor}.
     *
     * @param template the object to edit
     * @param parent   the parent object. May be {@code null}
     * @param context  the layout context. May be {@code null}
     * @throws ArchetypeServiceException for any archetype service error
     */
    public DocumentTemplateEditor(Entity template, IMObject parent,
                                  LayoutContext context) {
        super(template, parent, context);
        TemplateHelper helper = new TemplateHelper(ServiceHelper.getArchetypeService());
        Participation participation = helper.getDocumentParticipation(template);
        if (participation == null) {
            participation = (Participation) IMObjectCreator.create("participation.document");
        }
        participationEditor = ServiceHelper.getBean(IMObjectEditorFactory.class).create(
                participation, template, context);
        getEditors().add(participationEditor);

        if (participationEditor instanceof DocumentParticipationEditor) {
            getProperty("name").addModifiableListener(new ModifiableListener() {
                public void modified(Modifiable modifiable) {
                    onNameUpdated();
                }
            });

            // get the participation editor to delete the associated act
            // when the template is deleted
            ((DocumentParticipationEditor) participationEditor).setDeleteAct(true);
        }
    }

    /**
     * Creates the layout strategy.
     *
     * @return a new layout strategy
     */
    @Override
    protected IMObjectLayoutStrategy createLayoutStrategy() {
        return new DocumentTemplateLayoutStrategy(
                participationEditor.getComponent(),
                participationEditor.getFocusGroup());
    }

    /**
     * Invoked when the name is updated. Propagates the value to the associated
     * editor's {@link DocumentParticipationEditor#setDescription(String)}.
     */
    private void onNameUpdated() {
        String name = (String) getProperty("name").getValue();
        DocumentParticipationEditor editor
                = ((DocumentParticipationEditor) participationEditor);
        editor.setDescription(name);
    }
}