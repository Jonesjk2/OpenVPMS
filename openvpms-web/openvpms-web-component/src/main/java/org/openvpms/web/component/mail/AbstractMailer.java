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
 * Copyright 2015 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.web.component.mail;

import org.apache.commons.lang.StringUtils;
import org.openvpms.archetype.rules.doc.DocumentHandler;
import org.openvpms.archetype.rules.doc.DocumentHandlers;
import org.openvpms.component.business.domain.im.document.Document;
import org.openvpms.component.system.common.exception.OpenVPMSException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


/**
 * Abstract implementation of the {@link Mailer} interface.
 *
 * @author Tim Anderson
 */
public abstract class AbstractMailer implements Mailer {

    /**
     * The from address.
     */
    private String from;

    /**
     * The to addresses.
     */
    private String[] to;

    /**
     * The Cc addresses.
     */
    private String[] cc;

    /**
     * The Bcc addresses.
     */
    private String[] bcc;

    /**
     * The email subject.
     */
    private String subject;

    /**
     * The email body.
     */
    private String body;

    /**
     * The attachments.
     */
    private List<Document> attachments = new ArrayList<Document>();

    /**
     * The mail sender.
     */
    private final JavaMailSender sender;

    /**
     * The document handlers.
     */
    private final DocumentHandlers handlers;


    /**
     * Constructs an {@link AbstractMailer}.
     *
     * @param sender   the mail sender
     * @param handlers the document handlers
     */
    public AbstractMailer(JavaMailSender sender, DocumentHandlers handlers) {
        this.sender = sender;
        this.handlers = handlers;
    }

    /**
     * Sets the from address.
     *
     * @param from the from address
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the from address.
     *
     * @return the from address
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the to address.
     *
     * @param to the to addresses. May be {@code null}
     */
    @Override
    public void setTo(String[] to) {
        this.to = to;
    }

    /**
     * Returns the to addresses.
     *
     * @return the to addresses. May be {@code null}
     */
    @Override
    public String[] getTo() {
        return to;
    }

    /**
     * Sets the CC addresses.
     *
     * @param cc the CC addresses. May be {@code null}
     */
    @Override
    public void setCc(String[] cc) {
        this.cc = cc;
    }

    /**
     * Returns the CC addresses.
     *
     * @return the CC addresses. May be {@code null}
     */
    @Override
    public String[] getCc() {
        return cc;
    }

    /**
     * Sets the BCC addresses.
     *
     * @param bcc the BCC addresses. May be {@code null}
     */
    @Override
    public void setBcc(String[] bcc) {
        this.bcc = bcc;
    }

    /**
     * Returns the BCC addresses.
     *
     * @return the BCC addresses. May be {@code null}
     */
    @Override
    public String[] getBcc() {
        return bcc;
    }

    /**
     * Sets the subject.
     *
     * @param subject the subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Returns the subject.
     *
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the body.
     *
     * @param body the body
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Returns the body.
     *
     * @return the body
     */
    public String getBody() {
        return body;
    }

    /**
     * Adds an attachment.
     *
     * @param document the document to attach
     */
    public void addAttachment(Document document) {
        attachments.add(document);
    }

    /**
     * Sends the object to the default email address.
     *
     * @throws OpenVPMSException for any error
     */
    public void send() {
        MimeMessage message = sender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            populateMessage(helper);
            sender.send(message);
        } catch (OpenVPMSException exception) {
            throw exception;
        } catch (Throwable exception) {
            String address = (to != null) ? StringUtils.join(to, ", ") : null;
            throw new MailException(MailException.ErrorCode.FailedToSend, address, exception.getMessage());
        }
    }

    /**
     * Populates the mail message.
     *
     * @param helper the message helper
     * @throws MessagingException           for any messaging error
     * @throws UnsupportedEncodingException if the character encoding is not supported
     */
    protected void populateMessage(MimeMessageHelper helper)
            throws MessagingException, UnsupportedEncodingException {
        helper.setFrom(getFrom());
        String[] to = getTo();
        if (to != null && to.length != 0) {
            helper.setTo(to);
        }
        String[] cc = getCc();
        if (cc != null && cc.length != 0) {
            helper.setCc(cc);
        }
        String[] bcc = getBcc();
        if (bcc != null && bcc.length != 0) {
            helper.setBcc(bcc);
        }
        helper.setSubject(getSubject());
        if (body != null) {
            helper.setText(body);

        } else {
            helper.setText("");
        }
        for (Document attachment : attachments) {
            addAttachment(helper, attachment);
        }
    }

    /**
     * Helper to add an attachment.
     *
     * @param helper   the mime message helper
     * @param document the document to attach
     * @throws MessagingException for any error
     */
    protected void addAttachment(MimeMessageHelper helper, final Document document) throws MessagingException {
        final DocumentHandler handler = handlers.get(document.getName(), document.getArchetypeId().getShortName(),
                                                     document.getMimeType());
        helper.addAttachment(document.getName(), new InputStreamSource() {
            public InputStream getInputStream() {
                return handler.getContent(document);
            }
        });
    }
}
