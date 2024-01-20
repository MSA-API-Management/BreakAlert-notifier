package com.kornfeld.notifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSender {
    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String toEmail, String title, String body){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("d.kornfeld30@gmail.com");
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(title);
        mailSender.send(message);
        System.out.println("Email sent");
    }


}
