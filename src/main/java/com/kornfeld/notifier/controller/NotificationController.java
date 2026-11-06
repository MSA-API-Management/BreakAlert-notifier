package com.kornfeld.notifier.controller;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.DataChange;
import com.kornfeld.notifier.entities.Subscription;
import com.kornfeld.notifier.repositories.DataChangeRepository;
import com.kornfeld.notifier.repositories.SubscriptionRepository;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.EmailSender;
import com.kornfeld.notifier.services.GptAPIService;
import com.kornfeld.notifier.services.OpenApiExampleGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.json.JSONObject;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final GptAPIService gptApiService;
    private final OpenApiExampleGeneratorService openApiExampleGeneratorService;
    private final EmailSender emailSender;

    private final DataChangeRepository dataChangeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    private final RestTemplateBuilder restTemplateBuilder;

    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().softbreak("<br>").build();

    /**
     * Webhook Receiver: Generates API change summaries, persists them, and triggers notifications.
     * Expects multipart/form-data from CI/CD pipelines (e.g., GitLab).
     */
    @PostMapping(value = "/generate-response", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<String> generateResponse(
            @RequestPart("markdown") String markdownContent,
            @RequestPart(value = "oldSpec", required = false) String oldSpec,
            @RequestPart(value = "newSpec", required = false) String newSpec,
            @RequestParam("address") String address,
            @RequestParam("branch") String branch,
            @RequestParam("prevCommit") String previousCommitShortSha,
            @RequestParam("commit") String latestCommitShortSha
    ) {
        log.info("Received notification webhook for address: '{}' on branch: '{}'", address, branch);

        Subscription subscription = subscriptionRepository.findByAddressAndBranch(address, branch)
                .orElse(null);

        if (subscription == null) {
            log.warn("Discarding webhook: No matching subscription found in database.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Error: Address and branch combination not found in subscription list.");
        }

        log.info("Processing API diff for Subscription ID: {}", subscription.getId());

        String llmResponse = gptApiService.generateResponse(markdownContent);
        log.debug("LLM generated response summary successfully.");

        Set<String> modifiedEndpoints = extractModifiedEndpoints(markdownContent);
        Map<String, String> apiUsageExamples = openApiExampleGeneratorService.generateCurlExamples(newSpec, address, modifiedEndpoints);

        String cleanDetailedChanges = normalizeMarkdown(markdownContent);
        String finalHtmlContent = convertMarkdownToHtml(cleanDetailedChanges);
        boolean isBreaking = checkIfBackwardIncompatible(markdownContent);
        String title = "Change(s) in " + subscription.getSubName() + " detected";

        DataChange dataChange = new DataChange();
        dataChange.setTitle(title);
        dataChange.setContent(finalHtmlContent);
        dataChange.setChangeSummary(llmResponse);
        dataChange.setUsageExamples(apiUsageExamples);
        dataChange.setOpenApiOld(oldSpec);
        dataChange.setOpenApiNew(newSpec);
        dataChange.setLatestCommitShortSha(latestCommitShortSha);
        dataChange.setPrevCommitShortSha(previousCommitShortSha);
        dataChange.setBackwardIncompatible(isBreaking);
        dataChange.setSubscription(subscription);
        dataChange.setTimestamp(LocalDateTime.now());

        dataChangeRepository.save(dataChange);
        log.info("Successfully persisted DataChange record.");

        notifySubscribedUsers(subscription, dataChange);

        return ResponseEntity.ok("API Change was successfully processed.");
    }

    private void notifySubscribedUsers(Subscription subscription,
                                       DataChange dataChange) {
        // Stream all users and filter to only those subscribed to this specific repository
        List<AppUser> subscribedUsers = userRepository.findAll().stream()
                .filter(u -> u.getSubscriptions().contains(subscription))
                .toList();

        log.info("Found {} users subscribed to {}. Dispatching notifications.", subscribedUsers.size(), subscription.getSubName());

        for (AppUser user : subscribedUsers) {
            trySendEmailNotification(user, subscription, dataChange);
            trySendClientWebhookNotification(user, dataChange);
        }
    }

    private void trySendEmailNotification(AppUser user,
                                          Subscription subscription,
                                          DataChange dataChange) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            log.info("Queueing email notification for user: {} (Subscription: {})", user.getName(), subscription.getSubName());
            log.warn("WARNING: Email sending is currently DISABLED");
            // emailSender.sendEmail(user.getEmail(), dataChange.getTitle(), dataChange.getChangeSummary());
        }
    }

    private void trySendClientWebhookNotification(AppUser user,
                                                  DataChange dataChange) {
        if (user.getToken() != null && !user.getToken().isBlank()) {
            log.info("User '{}' is registered as a GitLab/Teams client. Preparing webhook dispatch.", user.getName());
            log.warn("WARNING: Ticket/Webhook creation is currently DISABLED");
            // sendPostRequest(user.getToken(), dataChange.getChangeSummary(), dataChange.getTitle());
        }
    }

    private void sendPostRequest(String apiUrl,
                                 String response,
                                 String title) {
        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", title);
        jsonObject.put("description", response);

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);

        try {
            log.debug("Dispatching outbound webhook to URL: {}", apiUrl);
            ResponseEntity<String> postResponse = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (postResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Outbound webhook dispatch successful. Response: {}", postResponse.getBody());
            } else {
                log.error("Outbound webhook dispatch failed. Status: {}, Response: {}", postResponse.getStatusCode(), postResponse.getBody());
            }
        } catch (RestClientException e) {
            log.error("Error occurred while making POST request to webhook URL {}: {}", apiUrl, e.getMessage());
        }
    }

    private boolean checkIfBackwardIncompatible(String markdown) {
        return markdown != null && !markdown.isEmpty() && markdown.toLowerCase().contains("broke backward compatibility");
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

        // Removes Project Name and first --- header block
        int firstSeparatorIndex = markdown.indexOf("---");
        String content = (firstSeparatorIndex != -1)
                ? markdown.substring(firstSeparatorIndex + 3)
                : markdown;

        return content.trim();
    }

    private Set<String> extractModifiedEndpoints(String markdown) {
        Set<String> endpoints = new HashSet<>();
        if (markdown == null || markdown.isEmpty()) return endpoints;

        String[] lines = markdown.split("\\r?\\n");
        boolean tracking = false;

        for (String line : lines) {
            String trimmed = line.replace("**", "").replace("#", "").replace("`", "").trim();

            if (trimmed.equalsIgnoreCase("What's New") || trimmed.equalsIgnoreCase("What's Changed")) {
                tracking = true;
                continue;
            } else if (trimmed.equalsIgnoreCase("What's Deleted") || trimmed.equalsIgnoreCase("What's Deprecated") || trimmed.equalsIgnoreCase("Result")) {
                tracking = false;
                continue;
            }

            if (tracking && (trimmed.startsWith("GET ") || trimmed.startsWith("POST ") ||
                    trimmed.startsWith("PUT ") || trimmed.startsWith("PATCH ") ||
                    trimmed.startsWith("DELETE "))) {
                endpoints.add(trimmed);
            }
        }
        return endpoints;
    }
}