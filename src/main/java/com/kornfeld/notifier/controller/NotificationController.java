package com.kornfeld.notifier.controller;

import com.kornfeld.notifier.services.EmailSender;
import com.kornfeld.notifier.services.Gpt35APIService;
import com.kornfeld.notifier.services.HTMLParser;
import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.DataChange;
import com.kornfeld.notifier.entities.Subscription;
import com.kornfeld.notifier.repositories.DataChangeRepository;
import com.kornfeld.notifier.repositories.SubscriptionRepository;
import com.kornfeld.notifier.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

    private final Gpt35APIService gpt3ApiService;
    private final HTMLParser htmlParser;
    @Autowired
    private EmailSender emailSender;

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    private DataChangeRepository dataChangeRepository;

    private SubscriptionRepository subscriptionRepository;

    private RestTemplate restTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public NotificationController(Gpt35APIService gpt3ApiService, HTMLParser htmlParser, RestTemplateBuilder restTemplate, DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.gpt3ApiService = gpt3ApiService;
        this.htmlParser = htmlParser;
        this.restTemplate = restTemplate.build();
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;

        this.markdownParser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().softbreak("<br>").build();
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
    @Transactional
    public ResponseEntity<String> generateResponse(
            @RequestBody String markdownContent,
            @RequestParam(value = "address") String address,
            @RequestParam(value = "branch") String branch,
            Model model
    ) throws IOException {
        Optional<Subscription> desiredSub = subscriptionRepository.findByAddressAndBranch(address, branch);

        if (desiredSub.isPresent()) {
            System.out.println("NOTIFICATION - Received a request successfully");
            System.out.println("NOTIFICATION - Markdown File: " + markdownContent);

//            String gptResponse = gpt3ApiService.generateResponse(markdownContent);
            String gptResponse = "Currently no summary";
            System.out.println("NOTIFICATION - Generated response:" + gptResponse);

            String cleanDetailedChanges = normalizeMarkdown(markdownContent);

            System.out.println("DEBUG - " + markdownContent);
            System.out.println("DEBUG2 - " + cleanDetailedChanges);

            String combinedMarkdown =
                    "### Summary\n" + gptResponse + "\n\n" +
                            "---\n" + cleanDetailedChanges + "\n\n" +
                            "---\n" +
                            "**Subscription Details:**\n" +
                            "(address = " + desiredSub.get().getAddress() + ", branch = " + desiredSub.get().getBranch() + ")";
            String finalHtmlContent = convertMarkdownToHtml(combinedMarkdown);
            String emailSubject = "Change(s) in " + desiredSub.get().getSubName() + " detected";

            DataChange dataChange = new DataChange();
            dataChange.setTitle(emailSubject);
            dataChange.setContent(finalHtmlContent);

            List<AppUser> users = userRepository.findAll();

            for (AppUser user : users) {
                Set<Subscription> subscriptions = user.getSubscriptions();
                for (Subscription subscription : subscriptions) {
                    if (subscription.getAddress().equals(address) && subscription.getBranch().equals(branch)) {
                        if (user.getEmail()!=null) {
                            System.out.println("NOTIFICATION - User: " + user.getName() + " receives an email, because of his subscription to: " + subscription.getSubName());
                            System.out.println("NOTIFICATION - Sending email... ");
                            System.out.println("NOTIFICATION - WARNING: EMAIL SENDING CURRENTLY DISABLED");
//                            emailSender.sendEmail(user.getEmail(), emailSubject, finalHtmlContent); // TODO not adapted for email responses
                        }

                        // Storing with multiple users is not a problem -> stores it only one time (because DataChange Object is created only once)
                        System.out.println("NOTIFICATION - Storing all info...");
                        storeDataChange(dataChange, subscription);
                        System.out.println("NOTIFICATION - Stored data change successfully");

                        if(user.getToken()!=null) {
                            System.out.println("NOTIFICATION - User is registered as a Client in GitLab");
                            // POST request for Teams and GitLab
                            String apiUrl = user.getToken();
                            System.out.println("NOTIFICATION - WARNING: TICKET CREATION CURRENTLY DISABLED");
//                            sendPostRequest(apiUrl, gptResponse, "API Changes"); // TODO new response not adapted for ticket description
                        }
                    }
                }
            }

        }
        return ResponseEntity.ok("API Change was successfully processed.");
    }

    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null) return "";
        return htmlRenderer.render(markdownParser.parse(markdown));
    }

    private String normalizeMarkdown(String raw) {
        if (raw == null) return "";

        // Removes Project Name and first ---
        int firstSeparatorIndex = raw.indexOf("---");
        String content = (firstSeparatorIndex != -1)
                ? raw.substring(firstSeparatorIndex + 3)
                : raw;

        return content.trim();
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
            System.out.println("NOTIFICATION - WebUrl: " + apiUrl);
            postResponse = restTemplate.postForEntity(apiUrl, entity, String.class);
            if (postResponse != null) {
                if (postResponse.getStatusCode().is2xxSuccessful()) {
                    System.out.println("NOTIFICATION - POST request successful. Response: " + postResponse.getBody());
                    successfulRequest = true;
                } else {
                    System.out.println("NOTIFICATION - POST request failed. Response: " + postResponse.getBody());
                }
            } else {
                System.out.println("NOTIFICATION - POST request failed. No response received.");
            }
        } catch (RestClientException e) {
            System.out.println("NOTIFICATION - Error while making POST request: " + e.getMessage());
        }
        return successfulRequest;
    }

    private void storeDataChange(DataChange dataChange, Subscription subscription){
        dataChange.setSubscription(subscription);
        dataChange.setTimestamp(LocalDateTime.now());
        dataChangeRepository.save(dataChange);
    }
}

