package com.clothingshop.service;

import com.clothingshop.util.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

/**
 * Sends email through the SMTP server configured with {@code mail.smtp.host}, {@code mail.smtp.port},
 * {@code mail.smtp.username}, {@code mail.smtp.password}, {@code mail.smtp.starttls} and {@code mail.from}.
 */
public class SmtpEmailService implements EmailService {

    private final Session session;
    private final String from;

    public SmtpEmailService() {
        Properties props = new Properties();
        props.put("mail.smtp.host", AppConfig.require("mail.smtp.host"));
        props.put("mail.smtp.port", AppConfig.get("mail.smtp.port", "587"));
        props.put("mail.smtp.starttls.enable", AppConfig.get("mail.smtp.starttls", "true"));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        String username = AppConfig.get("mail.smtp.username", "");
        String password = AppConfig.get("mail.smtp.password", "");
        Authenticator authenticator = null;
        if (!username.isEmpty()) {
            props.put("mail.smtp.auth", "true");
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }
        this.session = Session.getInstance(props, authenticator);
        this.from = AppConfig.get("mail.from", username);
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, true));
            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setText(body, StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            Transport.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Cannot send email to " + to, e);
        }
    }
}
