package net.jamnig.breakalert.controller;

import net.jamnig.breakalert.entities.AppUser;
import net.jamnig.breakalert.entities.DataChange;
import net.jamnig.breakalert.entities.Subscription;
import net.jamnig.breakalert.repositories.DataChangeRepository;
import net.jamnig.breakalert.repositories.SubscriptionRepository;
import net.jamnig.breakalert.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final UserRepository userRepository;
    private final DataChangeRepository dataChangeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RestTemplateBuilder restTemplateBuilder;

    /**
     * Initializes the main dashboard view, loading the user's active subscriptions and available subscriptions
     */
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String showDashboard(Model model,
                                @AuthenticationPrincipal AppUser appUser) {
        log.info("Initializing Dashboard for user: {}", appUser.getUsername());

        AppUser user = userRepository.findById(appUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Set<Subscription> userSubscriptions = user.getSubscriptions();
        model.addAttribute("username", user.getUsername());

        List<Subscription> availableSubscriptions = subscriptionRepository.findAll().stream()
                .filter(sub -> !userSubscriptions.contains(sub))
                .collect(Collectors.toList());
        model.addAttribute("toSubscribe", availableSubscriptions);

        if (user.getToken() != null && !user.getToken().isBlank()) {
            model.addAttribute("tokenExists", true);
        }

        Map<Long, List<DataChange>> dataChangesMap = new HashMap<>();
        for (Subscription subscription : userSubscriptions) {
            Set<DataChange> changes = dataChangeRepository.findBySubscriptionId(subscription.getId());
            if (changes != null && !changes.isEmpty()) {
                List<DataChange> sortedChanges = new ArrayList<>(changes);
                sortedChanges.sort((dc1, dc2) -> dc2.getTimestamp().compareTo(dc1.getTimestamp()));
                dataChangesMap.put(subscription.getId(), sortedChanges);
            }
        }

        List<Subscription> sortedSubscriptions = new ArrayList<>(userSubscriptions);
        sortedSubscriptions.sort(Comparator.comparing(Subscription::getSubName, String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("subscriptions", sortedSubscriptions);
        model.addAttribute("dataChangesMap", dataChangesMap);

        return "dashboard";
    }

    /**
     * Allows a user to subscribe to an existing global subscription
     */
    @PostMapping("/subscribe")
    @Transactional
    public String subscribe(@RequestParam Long subscriptionId,
                            @AuthenticationPrincipal AppUser appUser,
                            RedirectAttributes redirectAttributes) {
        log.info("User {} (ID={}) attempting to subscribe to subID={}", appUser.getUsername(), appUser.getId(), subscriptionId);

        AppUser user = userRepository.findById(appUser.getId()).orElseThrow();
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);

        if (subscriptionOpt.isPresent()) {
            user.getSubscriptions().add(subscriptionOpt.get());
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully subscribed to " + subscriptionOpt.get().getSubName());
        } else {
            log.warn("Subscription ID {} not found during subscribe attempt.", subscriptionId);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Subscription not found");
        }

        return "redirect:/dashboard";
//        return "redirect:/dashboard?tab=subscribe";
    }

    /**
     * Allows a user to unsubscribe from a global subscription
     */
    @DeleteMapping("/dashboard/subscriptions/{subscriptionId}")
    @Transactional
    public String unsubscribe(@PathVariable Long subscriptionId,
                              @AuthenticationPrincipal AppUser appUser,
                              RedirectAttributes redirectAttributes) {
        log.info("User {} attempting to unsubscribe from subID={}", appUser.getUsername(), subscriptionId);

        AppUser user = userRepository.findById(appUser.getId()).orElseThrow();
        Subscription subscriptionToRemove = subscriptionRepository.findById(subscriptionId).orElse(null);

        if (subscriptionToRemove != null && user.getSubscriptions().contains(subscriptionToRemove)) {
            user.getSubscriptions().remove(subscriptionToRemove);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("successMessage", "Successfully unsubscribed");
        } else {
            log.warn("Failed to unsubscribe: Subscription ID {} not found or user not subscribed.", subscriptionId);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Could not unsubscribe");
        }

        return "redirect:/dashboard";
    }

    /**
     * Saves and validates a GitLab webhook client URL for the user
     */
    @PostMapping("/saveToken")
    @Transactional
    public String saveToken(@RequestParam String tokenLink,
                            @AuthenticationPrincipal AppUser appUser,
                            RedirectAttributes redirectAttributes) {
        log.info("User {} attempting to save GitLab Client URL: {}", appUser.getUsername(), tokenLink);

        boolean isGitLabAvailable = testGitLabWebhook(tokenLink);

        if (isGitLabAvailable) {
            AppUser user = userRepository.findById(appUser.getId()).orElseThrow();
            user.setToken(tokenLink);
            userRepository.save(user);
            log.info("Successfully validated and saved GitLab token for user {}", user.getUsername());
        } else {
            log.warn("GitLab token validation failed for URL: {}", tokenLink);
        }

        redirectAttributes.addAttribute("gitLabAvailable", isGitLabAvailable);
        return "redirect:/clientConfirmation";
    }

    @GetMapping("/clientConfirmation")
    public String clientConfirmation(Model model,
                                     @RequestParam("gitLabAvailable") boolean gitLabAvailable) {
        if (gitLabAvailable) {
            model.addAttribute("urlCorrect", true);
        } else {
            model.addAttribute("urlWrong", true);
        }
        return "confirmClient";
    }

    /**
     * Downloads the old OpenAPI specification JSON for a specific data change
     */
    @GetMapping(value = "/download/old/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadOldOpenApiDesc(@PathVariable Long id) {
        DataChange change = dataChangeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change record not found"));

        if (change.getOpenApiOld() == null) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No old OpenAPI description available");
        }

        String fileName = String.format("old-openapi_%s.json", change.getPrevCommitShortSha());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(change.getOpenApiOld().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Downloads the new OpenAPI specification JSON for a specific data change
     */
    @GetMapping(value = "/download/new/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadNewOpenApiDesc(@PathVariable Long id) {
        DataChange change = dataChangeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change record not found"));

        if (change.getOpenApiNew() == null) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No new OpenAPI description available");
        }

        String fileName = String.format("new-openapi_%s.json", change.getLatestCommitShortSha());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(change.getOpenApiNew().getBytes(StandardCharsets.UTF_8));
    }

    private boolean testGitLabWebhook(String tokenLink) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", "Initialize Client");
        jsonObject.put("description", "Test connection from Notifier Dashboard");

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);

        try {
            ResponseEntity<String> postResponse = restTemplate.postForEntity(tokenLink, entity, String.class);
            return postResponse.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.error("Error validating GitLab URL {}: {}", tokenLink, e.getMessage());
            return false;
        }
    }
}
