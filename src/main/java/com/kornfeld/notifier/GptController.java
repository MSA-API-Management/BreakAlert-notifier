package com.kornfeld.notifier;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import java.time.LocalDateTime;

import java.util.Optional;


@Controller
public class GptController {

    private final Gpt3_5_API_Service gpt3ApiService;
    private final HTMLParser htmlParser;
    @Autowired
    private EmailSender emailSender;

    private DataChangeRepository dataChangeRepository;

    private SubscriptionRepository subscriptionRepository;

    private RestTemplate restTemplate;
    public GptController(Gpt3_5_API_Service gpt3ApiService, HTMLParser htmlParser, RestTemplateBuilder restTemplate, DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository) {
        this.gpt3ApiService = gpt3ApiService;
        this.htmlParser = htmlParser;
        this.restTemplate = restTemplate.build();
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
    }
    //Use RESTer tool to test it
    // or: C:\Users\kornd\OneDrive - Alpen-Adria Universität Klagenfurt\Dokumente\5. Semester WS23\Projekt\notifier>curl --location --request POST "http://localhost:8080/generate-response" --header "Content-Type: text/html" -d @./src/main/resources/api_changes.html
    @PostMapping("/generate-response")
    public String generateResponse(
            @RequestBody String htmlContent,
            Model model
    ) throws IOException {
        System.out.println("Received a request successfully");
        System.out.println("HTML File: " + htmlContent);

        String prompt = htmlParser.extractChanges(htmlContent);
        System.out.println("\n\nRequest: " + prompt);

        String response = gpt3ApiService.generateResponse(prompt);
        System.out.println("Response: \n"+response);

        /*
        String emailSubject = "API Changes";
        String emailBody = "Prompt:\n" + prompt + "\n\nResponse:\n\n" + response;
        String toEmail = "d.kornfeld11@gmail.com";  // Change to subscribed user emails

        System.out.println("Sending email... ");

        emailSender.sendEmail(toEmail, emailSubject, emailBody);


        System.out.println("Storing all info...");

        DataChange dataChange = new DataChange();
        dataChange.setTitle(emailSubject);
        dataChange.setContent(emailBody);
        Optional<Subscription> subscription = subscriptionRepository.findById(1L);
        if(subscription.isPresent()){
            dataChange.setSubscription(subscription.get());
        }
        dataChange.setTimestamp(LocalDateTime.now());

        dataChangeRepository.save(dataChange);

        System.out.println("Stored data change successfully");


        // Send POST request for Teams and GitLab
        String apiUrl = "https://gitlab.com/api/v4/projects/53346840/ref/main/trigger/pipeline?token=glptt-5f5619568b281e261c8cc546bdd22be10f001df9";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", "API Changes");
        jsonObject.put("description", response);

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);

        ResponseEntity<String> postResponse = restTemplate.postForEntity(apiUrl, entity, String.class);

        if (postResponse.getStatusCode().is2xxSuccessful()) {
            System.out.println("POST request successful. Response: " + postResponse.getBody());
        } else {
            System.out.println("POST request failed. Response: " + postResponse.getBody());
        }
        */

        return "index";
    }

}

