package com.kornfeld.notifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//create RESTful application
@RestController
public class EmailController {

    //form of dependency injection (created at runtime)

    /* Email test curl:
	curl -X POST -d "title=TestTitle&body=This is the body&toEmail=d.kornfeld11@gmail.com" http://localhost:8080/sendTest
	 */
    @Autowired
    private EmailSender emailSender;

    @PostMapping("/sendTest")
    public String sendTestEmail(
            @RequestParam String title,
            @RequestParam String body,
            @RequestParam String toEmail
    ) {
        System.out.println("sending...");
        emailSender.sendEmail(toEmail, title, body);
        return "Email sent";
    }
}
