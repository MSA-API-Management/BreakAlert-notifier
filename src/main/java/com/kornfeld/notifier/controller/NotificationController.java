package com.kornfeld.notifier.controller;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.DataChange;
import com.kornfeld.notifier.entities.Subscription;
import com.kornfeld.notifier.repositories.DataChangeRepository;
import com.kornfeld.notifier.repositories.SubscriptionRepository;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.EmailSender;
import com.kornfeld.notifier.services.GptAPIService;
import com.kornfeld.notifier.services.HTMLParser;
import com.kornfeld.notifier.services.OpenApiExampleGeneratorService;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Controller
public class NotificationController {

    private final GptAPIService gptApiService;
    private final OpenApiExampleGeneratorService openApiExampleGeneratorService;
    private final HTMLParser htmlParser;
    @Autowired
    private EmailSender emailSender;

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    private final DataChangeRepository dataChangeRepository;

    private final SubscriptionRepository subscriptionRepository;

    private RestTemplate restTemplate;

    private final UserRepository userRepository;

    public NotificationController(GptAPIService gptApiService,
                                  OpenApiExampleGeneratorService openApiExampleGeneratorService,
                                  HTMLParser htmlParser,
                                  RestTemplateBuilder restTemplate,
                                  DataChangeRepository dataChangeRepository,
                                  SubscriptionRepository subscriptionRepository,
                                  UserRepository userRepository) {
        this.gptApiService = gptApiService;
        this.openApiExampleGeneratorService = openApiExampleGeneratorService;
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

    // Example: curl --location --request POST "http://localhost:30004/generate-response?address=http.com&branch=main&prevCommit=dc3b2a1&commit=1a2b3cd" --form "markdown=<oas_diff/openapi_default.json.md" --form "oldSpec=<autooas-base/openapi_default.json" --form "newSpec=<autooas-head/openapi_default.json"
    @PostMapping(value = "/generate-response", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<String> generateResponse(
            @RequestPart("markdown") String markdownContent,
            @RequestPart(value = "oldSpec", required = false) String oldSpec,
            @RequestPart(value = "newSpec", required = false) String newSpec,
            @RequestParam(value = "address") String address,
            @RequestParam(value = "branch") String branch,
            @RequestParam(value = "prevCommit") String previousCommitShortSha,
            @RequestParam(value = "commit") String latestCommitShortSha,
            Model model
    ) {
        System.out.println("NOTIFICATION - Received request for address " + address + " branch " + branch);
        Optional<Subscription> desiredSub = subscriptionRepository.findByAddressAndBranch(address, branch);

        if (desiredSub.isPresent()) {
            System.out.println("NOTIFICATION - Received a request successfully");
            System.out.println("NOTIFICATION - Markdown File: " + markdownContent);

            String gptResponse = gptApiService.generateResponse(markdownContent);
            System.out.println("NOTIFICATION - Generated response: " + gptResponse);

            Set<String> modifiedEndpoints = extractModifiedEndpoints(markdownContent);
            String apiUsageExamples = openApiExampleGeneratorService.generateCurlExamples(newSpec, address, modifiedEndpoints);

            String cleanDetailedChanges = normalizeMarkdown(markdownContent);

//            String combinedMarkdown =
//                    "### Summary\n" + gptResponse + "\n\n" +
//                            "---\n" + cleanDetailedChanges + "\n\n" +
//                            "---\n" +
//                            "**API Examples (new version):**\n\n" +
//                            apiUsageExamples + "\n\n" +
//                            "---\n" +
//                            "**Subscription Details:**\n" +
//                            "(address = " + desiredSub.get().getAddress() + ", branch = " + desiredSub.get().getBranch() + ")";

            String finalHtmlContent = convertMarkdownToHtml(cleanDetailedChanges); // or combinedMarkdown
            String title = "Change(s) in " + desiredSub.get().getSubName() + " detected";
            boolean isBreaking = checkIfBackwardIncompatible(markdownContent);

            DataChange dataChange = new DataChange();
            dataChange.setTitle(title);
            dataChange.setContent(finalHtmlContent);
            dataChange.setChangeSummary(gptResponse);
            dataChange.setUsageExamples(apiUsageExamples);
            dataChange.setOpenApiOld(oldSpec);
            dataChange.setOpenApiNew(newSpec);
            dataChange.setLatestCommitShortSha(latestCommitShortSha);
            dataChange.setPrevCommitShortSha(previousCommitShortSha);
            dataChange.setBackwardIncompatible(isBreaking);

            List<AppUser> users = userRepository.findAll();

            for (AppUser user : users) {
                Set<Subscription> subscriptions = user.getSubscriptions();
                for (Subscription subscription : subscriptions) {
                    if (subscription.getAddress().equals(address) && subscription.getBranch().equals(branch)) {
                        System.out.println("NOTIFICATION - Storing all info...");
                        storeDataChange(dataChange, subscription);
                        System.out.println("NOTIFICATION - Stored data change successfully");

                        trySendEmailNotification(user, subscription, dataChange);
                        trySendSubscriptionNotification(user, dataChange);
                    }
                }
            }
            return ResponseEntity.ok("API Change was successfully processed.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: Address not found in subscription list!");
        }

    }

    private boolean checkIfBackwardIncompatible(String markdown) {
        if (markdown == null || markdown.isEmpty()) return false;

        String lowerCaseMd = markdown.toLowerCase();

        return lowerCaseMd.contains("broke backward compatibility");
    }

    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) return "";

        String html = htmlRenderer.render(markdownParser.parse(markdown));

        return html
                .replace("<h4>What's New</h4>", "<h4 id=\"whats-new\">What's New</h4>")
                .replace("<h4>What's Deleted</h4>", "<h4 id=\"whats-deleted\">What's Deleted</h4>")
                .replace("<h4>What's Deprecated</h4>", "<h4 id=\"whats-deprecated\">What's Deprecated</h4>")
                .replace("<h4>What's Changed</h4>", "<h4 id=\"whats-changed\">What's Changed</h4>");
    }

    private String normalizeMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) return "";

        // Removes Project Name and first ---
        int firstSeparatorIndex = markdown.indexOf("---");
        String content = (firstSeparatorIndex != -1)
                ? markdown.substring(firstSeparatorIndex + 3)
                : markdown;

        return content.trim();
    }

    // TODO rework
    private void trySendEmailNotification(AppUser user,
                                          Subscription subscription,
                                          DataChange dataChange) {
        if (user.getEmail() != null) {
            System.out.println("NOTIFICATION - User: " + user.getName() + " receives an email, because of his subscription to: " + subscription.getSubName());
            System.out.println("NOTIFICATION - Sending email... ");
            System.out.println("NOTIFICATION - WARNING: EMAIL SENDING CURRENTLY DISABLED");
//            emailSender.sendEmail(user.getEmail(), dataChange.getTitle(), dataChange.getChangeSummary());
        }
    }


    // Todo rework (also add jd)
    // POST request for Teams and GitLab
    private void trySendSubscriptionNotification(AppUser user,
                                                 DataChange dataChange) {
        if (user.getToken() != null) {
            String apiUrl = user.getToken();
            System.out.println("NOTIFICATION - User is registered as a Client in GitLab");
            System.out.println("NOTIFICATION - WARNING: TICKET CREATION CURRENTLY DISABLED");
            // sendPostRequest(apiUrl, dataChange.getChangeSummary();, "API Changes");
        }
    }

    private Set<String> extractModifiedEndpoints(String markdown) {
        Set<String> endpoints = new HashSet<>();

        if (markdown == null || markdown.isEmpty()) return endpoints;

        String[] lines = markdown.split("\\r?\\n");
        boolean tracking = false;

        for (String line : lines) {
            String cleanLine = line.replace("**", "")
                    .replace("#", "")
                    .replace("`", "");
            String trimmed = cleanLine.trim();

            if (trimmed.equalsIgnoreCase("What's New") || trimmed.equalsIgnoreCase("What's Changed")) {
                tracking = true;
                continue;
            } else if (trimmed.equalsIgnoreCase("What's Deleted") || trimmed.equalsIgnoreCase("What's Deprecated") || trimmed.equalsIgnoreCase("Result")) {
                tracking = false;
                continue;
            }

            if (tracking) {
                if (trimmed.startsWith("GET ") || trimmed.startsWith("POST ") ||
                        trimmed.startsWith("PUT ") || trimmed.startsWith("PATCH ") ||
                        trimmed.startsWith("DELETE ")) {

                    endpoints.add(trimmed);
                }
            }
        }

        return endpoints;
    }

    private void sendPostRequest(String apiUrl,
                                 String response,
                                 String title) {
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
                } else {
                    System.out.println("NOTIFICATION - POST request failed. Response: " + postResponse.getBody());
                }
            } else {
                System.out.println("NOTIFICATION - POST request failed. No response received.");
            }
        } catch (RestClientException e) {
            System.out.println("NOTIFICATION - Error while making POST request: " + e.getMessage());
        }
    }

    private void storeDataChange(DataChange dataChange,
                                 Subscription subscription) {
        dataChange.setSubscription(subscription);
        dataChange.setTimestamp(LocalDateTime.now());
        dataChangeRepository.save(dataChange);
    }
}

