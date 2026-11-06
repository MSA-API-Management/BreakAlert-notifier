package net.jamnig.breakalert.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSender {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public void sendEmail(String toEmail,
                          String title,
                          String body) {
        if (senderEmail == null || senderEmail.isBlank()) {
            log.warn("Email dispatch skipped for '{}': Mail credentials are not configured.", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject(title);
            message.setText(body);

            mailSender.send(message);
            log.info("Notification email sent successfully to: {}", toEmail);

        } catch (MailException e) {
            log.error("Failed to send notification email to '{}'. Reason: {}", toEmail, e.getMessage());
        }
    }
}
