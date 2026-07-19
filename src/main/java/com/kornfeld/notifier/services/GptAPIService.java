package com.kornfeld.notifier.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GptAPIService {
    private String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${GPT_API_KEY}")
    private String API_KEY;
    private static final String MODEL = "gpt-5-mini-2025-08-07";
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

    private static final String DESCRIPTION = "You are an assistant which analyzes the changes from APIs over time written in markdown. " +
            "Describe the most important/meaningful changes as briefly as you can. " +
            "The result should be a general textual overview of the changes including endpoint paths. Do not go into detail or make an enumeration. " +
            "The receiver of your answer is a developer, so there is no need for explanations on 'easy' topics but still keep it simple. " +
            "Make line breaks after sentences for a better structure. But do not make double line breaks.";

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

        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, requestEntity, String.class);

            ObjectMapper objectMapperResponse = new ObjectMapper();

            JsonNode jsonResponse = objectMapperResponse.readTree(responseEntity.getBody());
            JsonNode choicesNode = jsonResponse.path("choices").path(0);
            return choicesNode.path("message").path("content").asText();
        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("Quota/Rate limit exceeded: " + e.getResponseBodyAsString());
            return "API quota or rate limit exceeded.";
        } catch (HttpClientErrorException e) {
            System.err.println("OpenAI API client error (" + e.getStatusCode() + ") " + e.getResponseBodyAsString());
            return "Error communicating with OpenAI API.";
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
            return "Error processing API response.";
        } catch (RestClientException e) {
            System.err.println(e.getMessage());
            return "Error connecting to OpenAI API.";
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
//        requestBody.put("temperature", 0.7);
//        requestBody.put("max_tokens", 1000);
        return requestBody;
    }

    private static String convertRequestBodyToJSON(Map<String, Object> requestBody) {
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

