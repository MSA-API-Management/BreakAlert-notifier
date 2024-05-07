package com.kornfeld.notifier;

import com.kornfeld.notifier.services.EmailSender;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class EmailSenderTest {
    @Mock
    private JavaMailSender mailSender;
    @InjectMocks
    private EmailSender emailSender;

    private static final String fromEmail = "d.kornfeld30@gmail.com";
    private static final String toEmail = "recipient@test.com";
    private static final String title = "Testing sending an email";
    private static final String body = "This is a test email.";

    @Test
    public void testSendEmail() {
        emailSender.sendEmail(toEmail, title, body);

        verify(mailSender).send(createExpectedMessage());
    }

    private SimpleMailMessage createExpectedMessage() {
        SimpleMailMessage expectedMessage = new SimpleMailMessage();
        expectedMessage.setFrom(fromEmail);
        expectedMessage.setTo(toEmail);
        expectedMessage.setSubject(title);
        expectedMessage.setText(body);
        return expectedMessage;
    }
}

