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
 *  Copyright 2011 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id: $
 */

package org.openvpms.sms.mail;

import org.openvpms.sms.SMSException;


/**
 * Factory for messages to send to email-to-SMS providers.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: $
 */
public interface MailMessageFactory {

    /**
     * Creates a new email message to send to an SMS provider.
     *
     * @param phone the phone number to send the SMS to
     * @param text  the SMS text
     * @return a new email
     * @throws SMSException if the message cannot be created
     */
    MailMessage createMessage(String phone, String text);
}