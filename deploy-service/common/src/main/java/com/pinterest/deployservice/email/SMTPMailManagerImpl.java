/**
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.email;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class SMTPMailManagerImpl implements MailManager {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(SMTPMailManagerImpl.class);

    private String userName;

    private String password;

    /**
     * The e-mail address that Teletraan puts to "From:" field in outgoing e-mails.
     * Null if not configured.
     */
    private String adminAddress;

    /**
     * The SMTP server to use for sending e-mail. Null for default to the environment,
     * which is usually <tt>localhost</tt>.
     */
    private String host;
    /**
     * The SMTP port to use for sending e-mail. Null for default to the environment,
     * which is usually <tt>25</tt>.
     */
    private String port;

    private Properties properties;

    public SMTPMailManagerImpl(String host, String port, String userName, String password,
        String adminAddress, boolean sslEnabled) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.adminAddress = adminAddress;

        properties = new Properties();
        properties.setProperty("mail.transport.protocol", "smtp");
        if (!StringUtils.isEmpty(host)) {
            properties.setProperty("mail.smtp.host", host);
        }
        if (!StringUtils.isEmpty(port)) {
            properties.setProperty("mail.smtp.port", port);
        }
        if (!StringUtils.isEmpty(userName)) {
            properties.setProperty("mail.smtp.security", "true");
        } else {
            properties.setProperty("mail.smtp.security", "false");
        }

        if (sslEnabled) {
            properties.setProperty("mail.smtp.starttls.enable", "true");
        }
    }

    private Authenticator getAuthenticator() {
        if (!StringUtils.isEmpty(userName)) {
            return new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            };
        } else {
            return null;
        }
    }

    public void send(String to, String title, String message) throws Exception {
        Session session = Session.getDefaultInstance(properties, getAuthenticator());
        // Create a default MimeMessage object.
        MimeMessage mimeMessage = new MimeMessage(session);
        // Set From: header field of the header.
        mimeMessage.setFrom(new InternetAddress(adminAddress));
        // Set To: header field of the header.
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        // Set Subject: header field
        mimeMessage.setSubject(title);
        // Now set the actual message
        mimeMessage.setText(message);
        // Send message
        Transport.send(mimeMessage);
    }
}
