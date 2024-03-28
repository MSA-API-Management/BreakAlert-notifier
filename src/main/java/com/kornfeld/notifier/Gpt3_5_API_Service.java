package com.kornfeld.notifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

// Open API Key:
// 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
@Service
public class Gpt3_5_API_Service {
    private String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${GPT_API_KEY}")
    private String API_KEY;
    private static final String MODEL = "gpt-3.5-turbo";
    private RestTemplate restTemplate;

    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setAPI_KEY(String API_KEY) {
        this.API_KEY = API_KEY;
    }

    public void setOPENAI_API_URL(String OPENAI_API_URL) {
        this.OPENAI_API_URL = OPENAI_API_URL;
    }

    private static final String DESCRIPTION = "You are an assistant which analyzes the changes from APIs over time. " +
            "Usually you should receive a rather large list of API Changes. " +
            "Different changes in sections like 'What's New' are divided by ' | '. " +
            "Describe the most important/meaningful changes as briefly as you can. " +
            "The result should be a general textual overview of the changes. Do not go into detail or make a enumeration. " +
            "The receiver of your answer is a developer, so there is no need for explanations on 'easy' topics. " +
            "Make line breaks after sentences for a better structure. But do not make double line breaks. " +
            // ---------------- Optional ----------------
            "The result could look something like this: " +
            "The API now includes a new endpoint for testing with a null parameter. \n" +
            "The endpoint for testing has been updated to include a new parameter called 'testing'. \n" +
            "An endpoint labeled '2nd' has been removed. \n" +
            "An endpoint labeled 'second to last' is now deprecated. \n" +
            "The endpoint previously labeled 'last' has undergone some changes.";

    public String generateResponse(String prompt) {
        System.out.println("Generating Response soon...");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(API_KEY);

        Map<String, Object> requestBody = getRequestBody(prompt);

        String jsonBody = convertRequestBodyToJSON(requestBody);

        // Send to API
        RequestEntity<String> requestEntity = new RequestEntity<>(jsonBody, headers, HttpMethod.POST, null);

        restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, requestEntity, String.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            ObjectMapper objectMapperResponse = new ObjectMapper();
            try {
                JsonNode jsonResponse = objectMapperResponse.readTree(responseEntity.getBody());
                JsonNode choicesNode = jsonResponse.path("choices").path(0);
                return choicesNode.path("message").path("content").asText();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return "Error processing API response";
            }
        } else {
            return "Error communicating with GPT-3.5 API";
        }
    }

    private static Map<String, Object> getRequestBody(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Specifying role of the assistant
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", DESCRIPTION);
        messages.add(systemMessage);

        // Specifying the prompt
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        // Build request
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1000);
        return requestBody;
    }

    private static String convertRequestBodyToJSON(Map<String, Object> requestBody){
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Error converting request to JSON";
        }
        return jsonBody;
    }

}

