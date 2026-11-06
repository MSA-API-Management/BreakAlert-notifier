package net.jamnig.breakalert.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jamnig.breakalert.services.EmailSender;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EmailController {
    private final EmailSender emailSender;

    /**
     * Test endpoint to verify email functionality.
     * Protected to ensure only authenticated admins can trigger outbound emails.
     * <p>
     * Test Example (Requires Admin JWT Cookie):
     * curl -X POST -H "Cookie: jwt=..." -d "title=Test&body=Body&toEmail=test@example.com" http://localhost:30004/sendTest
     */
    @PostMapping("/sendTest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> sendTestEmail(@RequestParam String title,
                                                @RequestParam String body,
                                                @RequestParam String toEmail) {
        log.info("Admin initiated a test email dispatch to: {}", toEmail);
        emailSender.sendEmail(toEmail, title, body);
        return ResponseEntity.ok("Test email dispatched.");
    }
}
