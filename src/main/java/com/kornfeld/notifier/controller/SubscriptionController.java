package com.kornfeld.notifier.controller;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.Subscription;
import com.kornfeld.notifier.repositories.SubscriptionRepository;
import com.kornfeld.notifier.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    // To test in windows, call with:
    // ...\Projekt\notifier> curl -X POST -H "Content-Type: application/json" -d "[{\"subName\": \"Subscription1\", \"address\": \"http.com\", \"branch\": \"main\"}, {\"subName\": \"Subscription2\", \"address\": \"http.test.com\", \"branch\": \"main\"}]" http://localhost:8080/subscriptions/processAll?webUrl=https://gitlab.com/api/v4/projects/53346840/ref/main/trigger/pipeline?token=glptt-5f5619568b281e261c8cc546bdd22be10f001df9

    /**
     * Webhook Endpoint: Processes JSON lists from GitLab clients.
     * Accessible without JWT, authenticated via the 'webUrl' token parameter.
     */
    // TODO security risk?
    @PostMapping("/processAll")
    @Transactional
    public ResponseEntity<String> processJson(@RequestBody List<Subscription> incomingSubscriptions,
                                              @RequestParam("webUrl") String webUrl) {
        log.info("Processing webhook for client webUrl token: {}", webUrl);

        AppUser user = userRepository.findAll().stream()
                .filter(u -> webUrl.equals(u.getToken()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("Token not found in DB. Creating new client user.");
                    AppUser newUser = new AppUser();
                    newUser.setToken(webUrl);
                    return userRepository.save(newUser);
                });

        syncClientSubscriptions(user, incomingSubscriptions);
        userRepository.save(user);

        return ResponseEntity.ok("JSON-Data processed successfully");
    }


    /**
     * API Endpoint: Add a new subscription via JSON (Admin Only)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<String> addNewSubscriptionJson(@RequestBody Subscription subscription) {
        try {
            validateAndSaveSubscription(subscription);
            return ResponseEntity.status(HttpStatus.CREATED).body("Subscription successfully added");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /**
     * UI Endpoint: Add a new subscription via Dashboard Form (Admin Only)
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public String addNewSubscriptionForm(@ModelAttribute Subscription subscription,
                                         RedirectAttributes redirectAttributes) {
        log.info("Admin attempting to add new subscription via form: {}", subscription.getSubName());
        try {
            validateAndSaveSubscription(subscription);
            redirectAttributes.addFlashAttribute("successMessage", "Subscription successfully added!");
        } catch (Exception e) {
            log.error("Failed to add subscription", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/dashboard?tab=subscribe";
    }

    /**
     * UI Endpoint: Edit Subscription Name (Admin Only)
     */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public String editSubscriptionName(@PathVariable Long id,
                                       @RequestParam String newName,
                                       RedirectAttributes redirectAttributes) {
        log.info("Admin attempting to rename subscription ID {} to '{}'", id, newName);

        subscriptionRepository.findById(id).ifPresentOrElse(sub -> {
            sub.setSubName(newName.trim());
            subscriptionRepository.save(sub);
            redirectAttributes.addFlashAttribute("successMessage", "Subscription name updated successfully!");
        }, () -> redirectAttributes.addFlashAttribute("errorMessage", "Error: Subscription not found"));

        return "redirect:/dashboard?tab=subscribe";
    }

    /**
     * UI Endpoint: Delete Global Subscription (Admin Only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public String deleteGlobalSubscription(@PathVariable Long id,
                                           RedirectAttributes redirectAttributes) {
        log.info("Admin attempting to delete global subscription ID {}", id);

        Subscription sub = subscriptionRepository.findById(id).orElse(null);
        if (sub != null) {
            userRepository.findAll().forEach(u -> {
                if (u.getSubscriptions().remove(sub)) {
                    userRepository.save(u);
                }
            });

            subscriptionRepository.delete(sub);
            redirectAttributes.addFlashAttribute("successMessage", "Subscription deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Subscription not found");
        }

        return "redirect:/dashboard?tab=subscribe";
    }

    private void syncClientSubscriptions(AppUser user,
                                         List<Subscription> incomingSubscriptions) {
        List<Subscription> resolvedIncoming = incomingSubscriptions.stream()
                .map(this::findOrCreateSubscription)
                .toList();

        if (user.getEmail() == null && user.getName() == null && user.getPassword() == null) {
            user.getSubscriptions().retainAll(resolvedIncoming);
        }

        user.getSubscriptions().addAll(resolvedIncoming);
    }

    private Subscription findOrCreateSubscription(Subscription incomingData) {
        String cleanAddress = incomingData.getAddress().trim();
        String cleanBranch = incomingData.getBranch().trim();

        return subscriptionRepository.findByAddressAndBranch(cleanAddress, cleanBranch)
                .orElseGet(() -> {
                    incomingData.setId(null);
                    incomingData.setAddress(cleanAddress);
                    incomingData.setBranch(cleanBranch);
                    log.info("Creating new global subscription dynamically from webhook: {} ({})", incomingData.getSubName(), cleanAddress);
                    return subscriptionRepository.save(incomingData);
                });
    }

    private void validateAndSaveSubscription(Subscription subscription) {
        if (subscription == null || subscription.getAddress() == null || subscription.getBranch() == null || subscription.getSubName() == null) {
            throw new IllegalArgumentException("Subscription address, branch, and name are required.");
        }

        String cleanedAddress = subscription.getAddress().trim();
        String cleanedBranch = subscription.getBranch().trim();

        if (subscriptionRepository.findByAddressAndBranch(cleanedAddress, cleanedBranch).isPresent()) {
            throw new IllegalStateException("A subscription with this address and branch already exists.");
        }

        subscription.setId(null);
        subscription.setAddress(cleanedAddress);
        subscription.setBranch(cleanedBranch);
        subscriptionRepository.save(subscription);
        log.info("Successfully saved new global subscription: {}", cleanedAddress);
    }
}


