package com.kornfeld.notifier.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSender {
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendEmail(String toEmail, String title, String body){
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setText(body);
            message.setSubject(title);
            mailSender.send(message);
            System.out.println("NOTIFICATION - Email sent successfully to: " + toEmail);
        } catch (MailException e) {
            System.err.println("ERROR - Email could not be sent to: " + toEmail + ". Reason: " + e.getMessage());
        }
    }
}
