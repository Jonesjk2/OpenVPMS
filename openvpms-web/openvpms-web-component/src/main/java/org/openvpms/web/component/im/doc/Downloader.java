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
 * Copyright 2013 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.web.component.im.doc;

import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.app.Button;
import nextapp.echo2.app.Component;
import org.apache.commons.io.FilenameUtils;
import org.openvpms.archetype.rules.doc.DocumentException;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.document.Document;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.report.openoffice.OpenOfficeException;
import org.openvpms.web.echo.servlet.DownloadServlet;


/**
 * Helper to render a component to download a document.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public abstract class Downloader {

    /**
     * Default download button style.
     */
    protected static final String DEFAULT_BUTTON_STYLE = "download.default";

    /**
     * The listener for events.
     */
    private DownloaderListener listener;

    /**
     * The maximum file name display length, or {@code -1} to display the entire file name.
     */
    private int nameLength = -1;


    /**
     * Registers a listener to be notified when the link is clicked.
     * <p/>
     * When registered, this overrides the default behaviour of downloading documents.
     *
     * @param listener the listener. May be <tt>null</tt>
     */
    public void setListener(DownloaderListener listener) {
        this.listener = listener;
    }

    /**
     * Returns a component representing the downloader.
     *
     * @return the component
     */
    public abstract Component getComponent();

    /**
     * Initiates download of the document.
     *
     * @throws ArchetypeServiceException for any archetype service error
     * @throws DocumentException         if the document can't be found
     * @throws OpenOfficeException       if the document cannot be converted
     */
    public void download() {
        download(null);
    }

    /**
     * Initiates download of the document after converting it to the specified type.
     *
     * @param mimeType the mimetype. If <tt>null</tt>, indicates no conversion is required
     * @throws ArchetypeServiceException for any archetype service error
     * @throws DocumentException         if the document can't be found
     * @throws OpenOfficeException       if the document cannot be converted
     */
    public void download(String mimeType) {
        Document document = getDocument(mimeType);
        DownloadServlet.startDownload(document);
    }

    /**
     * Shortens long names to the specified number of characters.
     *
     * @param length the maximum length. Must be {@code > 5}
     */
    public void setNameLength(int length) {
        if (length > 5) {
            this.nameLength = length;
        }
    }

    /**
     * Returns the document for download.
     *
     * @param mimeType the expected mime type. If <tt>null</tt>, then no conversion is required.
     * @return the document for download
     * @throws ArchetypeServiceException for any archetype service error
     * @throws DocumentException         if the document can't be found
     * @throws OpenOfficeException       if the document cannot be converted
     */
    protected abstract Document getDocument(String mimeType);

    /**
     * Invoked when the document is selected. If a listener is registered, this will be notified, otherwise
     * {@link #download} will be called.
     *
     * @param mimeType the expected mimetype. If <tt>null</tt>, indicates no conversion is required
     * @throws ArchetypeServiceException for any archetype service error
     * @throws DocumentException         if the document can't be found
     * @throws OpenOfficeException       if the document cannot be converted
     */
    protected void selected(String mimeType) {
        if (listener != null) {
            listener.download(this, mimeType);
        } else {
            download(mimeType);
        }
    }

    /**
     * Returns a document, given its reference.
     *
     * @param reference the document reference
     * @param mimeType  the expected mime type. If <tt>null</tt>, then no conversion is required.
     * @return the document
     * @throws ArchetypeServiceException for any archetype service error
     * @throws DocumentException         if the document can't be found
     * @throws OpenOfficeException       if the document cannot be converted
     */
    protected Document getDocumentByRef(IMObjectReference reference, String mimeType) {
        IArchetypeService service = ArchetypeServiceHelper.getArchetypeService();
        Document result = (Document) service.get(reference);
        if (result == null) {
            throw new DocumentException(DocumentException.ErrorCode.NotFound);
        }
        if (mimeType != null && !mimeType.equals(result.getMimeType())) {
            result = DocumentHelper.convert(result, mimeType);
        }
        return result;
    }

    /**
     * Helper to set the button style.
     *
     * @param button the button
     * @param name   the button name. Long names will be shortened if {@link #nameLength} is > 0}
     */
    protected void setButtonStyle(Button button, String name) {
        String tooltip = null;
        if (nameLength > 0 && name.length() > nameLength) {
            tooltip = name;
            int start = nameLength / 2 - 1;
            int end = nameLength - start - 3;
            name = name.substring(0, start) + "..." + name.substring(name.length() - end);
        }
        button.setText(name);
        if (tooltip != null) {
            button.setToolTipText(tooltip);
        }
        String styleName = getStyleName(name);
        button.setStyleName(styleName);
    }

    /**
     * Helper to determine the button style name from a file name.
     *
     * @param name the file name. May be <tt>null</tt>
     * @return the button style name, or {@link #DEFAULT_BUTTON_STYLE} if the style is not known
     */
    protected String getStyleName(String name) {
        String styleName;
        if (name != null) {
            String ext = FilenameUtils.getExtension(name).toLowerCase();
            styleName = "download." + ext;
            ApplicationInstance active = ApplicationInstance.getActive();
            if (active.getStyle(Button.class, styleName) == null) {
                styleName = DEFAULT_BUTTON_STYLE;
            }
        } else {
            styleName = DEFAULT_BUTTON_STYLE;
        }
        return styleName;
    }

}
