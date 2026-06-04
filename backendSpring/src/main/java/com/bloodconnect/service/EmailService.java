package com.bloodconnect.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromName;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from-name}") String fromName,
                        @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromName = fromName;
        this.fromAddress = fromAddress;
    }

    /** Sends HTML mail. No-ops quietly if SMTP credentials are not configured. */
    public void sendHtml(String to, String subject, String html) {
        if (fromAddress == null || fromAddress.isBlank()) {
            System.out.println("[EMAIL] (disabled - no EMAIL_USER) would send to " + to + ": " + subject);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Email error: " + e.getMessage());
        }
    }
}
