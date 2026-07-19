package com.kornfeld.notifier;

import com.kornfeld.notifier.services.GptAPIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;

public class GptAPIServiceTest {
    private static final String prompt = "\nWhat's New:\nGET/Test null \n\nWhat's Deleted:\n\n\nWhat's Deprecated:\n\n\nWhat's Changed:\n";
    private RestTemplate restTemplateMock;
    private GptAPIService gpt3ApiService;
    @BeforeEach
    public void setUp() {
        restTemplateMock = Mockito.mock(RestTemplate.class);
        gpt3ApiService = new GptAPIService();
        gpt3ApiService.setRestTemplate(restTemplateMock);
        gpt3ApiService.setAPI_KEY("");
    }

    /*@Test
    public void testGenerateResponse_Successful() {
        // Stubbing the restTemplateMock.exchange() method call for a successful response
        Mockito.when(restTemplateMock.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class), Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok().body("{\"test\": [{\"test123\": {\"content\": \"Response message\"}}]}"));
        String generatedResponse = gpt3ApiService.generateResponse(prompt);
        assertNotNull(generatedResponse);
        assertFalse(generatedResponse.isBlank());
    }*/

    @Test
    public void testGenerateResponse_ErrorProcessingApiResponse() {
        Mockito.when(restTemplateMock.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class), Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok().body("Invalid JSON Response"));

        gpt3ApiService.setOPENAI_API_URL("http://example.com");

        String generatedResponse = gpt3ApiService.generateResponse(prompt);

        assertEquals("Error processing API response", generatedResponse);
    }
}
