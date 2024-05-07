package com.kornfeld.notifier;

import com.kornfeld.notifier.services.EmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private EmailSender emailSender;

    @Test
    public void testSendTestEmail() throws Exception {
        String title = "TestTitle";
        String body = "This is the body";
        String toEmail = "test@example.com";

        mockMvc.perform(post("/sendTest")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("title", title)
                        .param("body", body)
                        .param("toEmail", toEmail))
                .andExpect(status().isOk());

        verify(emailSender).sendEmail(toEmail, title, body);
    }
}
