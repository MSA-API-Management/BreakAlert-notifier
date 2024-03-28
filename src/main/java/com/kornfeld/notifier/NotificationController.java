package com.kornfeld.notifier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Controller
public class NotificationController {

    private final Gpt3_5_API_Service gpt3ApiService;
    private final HTMLParser htmlParser;
    @Autowired
    private EmailSender emailSender;

    private DataChangeRepository dataChangeRepository;

    private SubscriptionRepository subscriptionRepository;

    private RestTemplate restTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public NotificationController(Gpt3_5_API_Service gpt3ApiService, HTMLParser htmlParser, RestTemplateBuilder restTemplate, DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.gpt3ApiService = gpt3ApiService;
        this.htmlParser = htmlParser;
        this.restTemplate = restTemplate.build();
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }

    public void setRestTemplate(RestTemplateBuilder restTemplate) {
        this.restTemplate = restTemplate.build();
    }

    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    //Use RESTer tool to test it
    // or: C:\Users\kornd\OneDrive - Alpen-Adria Universität Klagenfurt\Dokumente\5. Semester WS23\Projekt\notifier>curl --location --request POST "http://localhost:30004/generate-response?address=test3.com&branch=main" --header "Content-Type: text/html" -d @./src/main/resources/api_changes.html
    @PostMapping("/generate-response")
    public ResponseEntity<String> generateResponse(
            @RequestBody String htmlContent,
            @RequestParam(value = "address") String address,
            @RequestParam(value = "branch") String branch,
            Model model
    ) throws IOException {
        Optional<Subscription> desiredSub = subscriptionRepository.findByAddressAndBranch(address, branch);

        if (desiredSub.isPresent()) {
            System.out.println("Received a request successfully");
            System.out.println("HTML File: " + htmlContent);

            String prompt = htmlParser.extractChanges(htmlContent);
            System.out.println("\n\nRequest: " + prompt);

            String response = gpt3ApiService.generateResponse(prompt);
            System.out.println("Response: \n" + response);


            String emailSubject = "Change(s) in "+desiredSub.get().getSubName()+" detected";
            String emailBody = "Overview:\n\n" + response + "\n\n\nDetailed Changes:\n" + prompt + "\n\nSubscription Details:\n(address = " + desiredSub.get().getAddress() + ", branch = " + desiredSub.get().getBranch() + ")";

            DataChange dataChange = new DataChange();
            dataChange.setTitle(emailSubject);
            dataChange.setContent(emailBody);

            List<AppUser> users = userRepository.findAll();

            for (AppUser user : users) {
                Set<Subscription> subscriptions = user.getSubscriptions();
                for (Subscription subscription : subscriptions) {
                    if (subscription.getAddress().equals(address) && subscription.getBranch().equals(branch)) {
                        if (user.getEmail()!=null) {
                            System.out.println("User: " + user.getName() + " receives an email, because of his subscription to: " + subscription.getSubName());
                            System.out.println("Sending email... ");
                            emailSender.sendEmail(user.getEmail(), emailSubject, emailBody);
                        }

                        // Storing with multiple users is not a problem -> stores it only one time (because DataChange Object is created only once)
                        System.out.println("Storing all info...");
                        storeDataChange(dataChange, subscription);
                        System.out.println("Stored data change successfully");

                        if(user.getToken()!=null) {
                            System.out.println("User is registered as a Client in GitLab");
                            // POST request for Teams and GitLab
                            String apiUrl = user.getToken();
                            sendPostRequest(apiUrl, response, "API Changes");
                        }
                    }
                }
            }

        }
        return ResponseEntity.ok("API Change was successfully processed.");
    }

    private boolean sendPostRequest(String apiUrl, String response, String title) {
        boolean successfulRequest = false;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", title);
        jsonObject.put("description", response);

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);
        ResponseEntity<String> postResponse;
        try {
            System.out.println("webUrl: " + apiUrl);
            postResponse = restTemplate.postForEntity(apiUrl, entity, String.class);
            if (postResponse != null) {
                if (postResponse.getStatusCode().is2xxSuccessful()) {
                    System.out.println("POST request successful. Response: " + postResponse.getBody());
                    successfulRequest = true;
                } else {
                    System.out.println("POST request failed. Response: " + postResponse.getBody());
                }
            } else {
                System.out.println("POST request failed. No response received.");
            }
        } catch (RestClientException e) {
            System.out.println("Error while making POST request: " + e.getMessage());
        }
        return successfulRequest;
    }

    private void storeDataChange(DataChange dataChange, Subscription subscription){
        dataChange.setSubscription(subscription);
        dataChange.setTimestamp(LocalDateTime.now());
        dataChangeRepository.save(dataChange);
    }

}

