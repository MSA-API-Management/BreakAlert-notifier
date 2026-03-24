package com.kornfeld.notifier.controller;
import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.DataChange;
import com.kornfeld.notifier.entities.Subscription;
import com.kornfeld.notifier.repositories.DataChangeRepository;
import com.kornfeld.notifier.repositories.SubscriptionRepository;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.JWTService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.coyote.Response;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
public class DashboardController {
    private final UserRepository userRepository;
    private final DataChangeRepository dataChangeRepository;
    private final SubscriptionRepository subscriptionRepository;
    @PersistenceContext
    private EntityManager entityManager;
    private RestTemplate restTemplate;

    private final JWTService jwtService;

    @Autowired
    public DashboardController(UserRepository userRepository, DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository, RestTemplateBuilder restTemplate, JWTService jwtService) {
        this.userRepository = userRepository;
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.restTemplate = restTemplate.build();
        this.jwtService = jwtService;
    }

    public void setRestTemplate(RestTemplateBuilder restTemplate) {
        this.restTemplate = restTemplate.build();
    }

    public void setEntityManager(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    @GetMapping("/dashboard")
    @Transactional
    public String showDashboard(Model model, @AuthenticationPrincipal AppUser appUser) {
        System.out.println("DASHBOARD - Initializing Dashboard for: " + appUser.getName());

        appUser = entityManager.merge(appUser);

        Set<Subscription> subscriptions = appUser.getSubscriptions();
        model.addAttribute("username", appUser.getName());

        List<Subscription> toSubscribe = subscriptionRepository.findAll();
        toSubscribe.removeAll(subscriptions);
        model.addAttribute("toSubscribe", toSubscribe);

        if(appUser.getToken() != null && !appUser.getToken().isEmpty()){
            model.addAttribute("tokenExists", true);
        }

        Map<Long, List<DataChange>> dataChangesMap = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            System.out.println("DASHBOARD - Load Subscription ID" + subscription.getId());
            Set<DataChange> dataChanges = dataChangeRepository.findBySubscriptionId(subscription.getId());

            if(dataChanges != null) {
                List<DataChange> dataChangeList = new ArrayList<>(dataChanges);
                dataChangeList.sort((dc1, dc2) -> dc2.getTimestamp().compareTo(dc1.getTimestamp()));
                dataChangesMap.put(subscription.getId(), dataChangeList);
            }
        }

        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("dataChangesMap", dataChangesMap);

        return "dashboard";
    }

    @PostMapping("/subscribe")
    @Transactional
    public String subscribe(@RequestParam Long subscriptionId, @AuthenticationPrincipal AppUser appUser, Model model, RedirectAttributes redirectAttributes) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        appUser = entityManager.merge(appUser);
        entityManager.persist(appUser);

        System.out.println("DASHBOARD - User " + appUser.getName() + "(ID=" + appUser.getId()+") wants to subscribe to subID="+subscriptionId);

        if (subscription != null) {
            appUser.getSubscriptions().add(subscription);
            model.addAttribute("subscriptionSuccess", true);
        } else {
            model.addAttribute("subscriptionError", true);
        }

        Set<Subscription> subscriptions = appUser.getSubscriptions();
        List<Subscription> toSubscribe = subscriptionRepository.findAll();
        toSubscribe.removeAll(subscriptions);
        model.addAttribute("toSubscribe", toSubscribe);

        return "redirect:/dashboard";
    }

    @PostMapping("/saveToken")
    @Transactional
    public String saveToken(@RequestParam String tokenLink,@AuthenticationPrincipal AppUser appUser, RedirectAttributes redirectAttributes, Model model) {

        // For testing: https://gitlab.com/api/v4/projects/53346840/ref/main/trigger/pipeline?token=glptt-5f5619568b281e261c8cc546bdd22be10f001df9

        boolean gitLabAvailable = false;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("title", "Initialize Client");
        jsonObject.put("description", "Test");

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);
        ResponseEntity<String> postResponse;
        try {
            System.out.println("DASHBOARD - Trying to Save GitLab Client URL: " + tokenLink);
            postResponse = restTemplate.postForEntity(tokenLink, entity, String.class);
            if (postResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("DASHBOARD - Test POST request successful. Response: " + postResponse.getBody());
                gitLabAvailable = true;
            } else {
                System.out.println("DASHBOARD - Test POST request failed. Response: " + postResponse.getBody());
                System.out.println("DASHBOARD - Try again later or enter a different URL.");
                model.addAttribute("urlFalse", true);
            }
        } catch (RestClientException e) {
            System.out.println("DASHBOARD - Error while making Test POST request: " + e.getMessage());
            System.out.println("DASHBOARD - Try again later or enter a different URL.");
            model.addAttribute("urlFalse", true);
        }

        if(gitLabAvailable){
            appUser = entityManager.merge(appUser);
            entityManager.persist(appUser);
            appUser.setToken(tokenLink);
            userRepository.save(appUser);
        }
        model.addAttribute("urlFalse", false);
        redirectAttributes.addAttribute("gitLabAvailable", gitLabAvailable);
        return "redirect:/clientConfirmation";
    }

    @GetMapping("/clientConfirmation")
    public String clientConfirmation(Model model, @AuthenticationPrincipal AppUser appUser, @RequestParam("gitLabAvailable") boolean gitLabAvailable, RedirectAttributes redirectAttributes) {
        if (gitLabAvailable){
            model.addAttribute("urlCorrect", true);
        } else {
            model.addAttribute("urlWrong", true);
        }

        return "confirmClient";
    }


    @PostMapping("/dashboard/deleteSubscription/{subscriptionId}")
    @Transactional
    public String deleteSubscription(@PathVariable Long subscriptionId, @AuthenticationPrincipal AppUser appUser, Model model, RedirectAttributes redirectAttributes) {
        Optional<Subscription> subscription = subscriptionRepository.findById(subscriptionId);
        appUser = entityManager.merge(appUser);
        entityManager.persist(appUser);

        if(subscription.isPresent()){
            appUser.getSubscriptions().remove(subscription.get());
            System.out.println("DASHBOARD - Successfully deleted the subscription ID " + subscriptionId);
        }else{
            System.out.println("DASHBOARD - Error when deleting the subscription ID " + subscriptionId);
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/download/old/{id}")
    public ResponseEntity<byte[]> downloadOldOpenApiDesc(@PathVariable Long id) {
        DataChange change = dataChangeRepository.findById(id).orElseThrow(() ->new ResponseStatusException(HttpStatus.NOT_FOUND));

        String json = change.getOpenApiOld();

        if (json == null){
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No OpenAPI description available");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=old-openapi.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/download/new/{id}")
    public ResponseEntity<byte[]> downloadNewOpenApiDesc(@PathVariable Long id) {
        DataChange change = dataChangeRepository.findById(id).orElseThrow(() ->new ResponseStatusException(HttpStatus.NOT_FOUND));

        String json = change.getOpenApiNew();

        if (json == null){
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "No OpenAPI description available");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=new-openapi.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    // TODO IDOR RISK (also for other endpoints)
    @PostMapping("/dashboard/changeSubscriptionName/{subscriptionId}")
    @Transactional
    public String changeSubscriptionName(@PathVariable Long subscriptionId, @RequestParam String newName, @AuthenticationPrincipal AppUser appUser, Model model, RedirectAttributes redirectAttributes) {
        appUser = entityManager.merge(appUser);

        if(appUser!=null) {
            Optional<Subscription> subscriptionOptional = subscriptionRepository.findById(subscriptionId);

            if (subscriptionOptional.isPresent()) {
                Subscription subscription = subscriptionOptional.get();
                subscription.setSubName(newName);
                subscriptionRepository.save(subscription);
                System.out.println("DASHBOARD - Successfully changed the name of the subscription ID " + subscriptionId);
            } else {
                System.out.println("DASHBOARD - Error when changing the name of the subscription ID " + subscriptionId);
            }
        }

        return "redirect:/dashboard";
    }

    private AppUser getCurrentUser(String jwtToken){
        boolean tokenTimeValid = jwtService.isTokenTimeValid(jwtToken);
        AppUser user = null;
        if(tokenTimeValid) {
            String username = jwtService.getUsername(jwtToken);
            user = userRepository.findByName(username);
            if(user!=null) {
                System.out.println("DASHBOARD - Valid Token from user " + user.getName() + "(ID=" + user.getId() + ")");
                setAuthentication(user);
            } else {
                System.out.println("DASHBOARD - Token is not valid");
            }
        }
        return user;
    }

    private void setAuthentication(AppUser user){
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // To receive info about current user:
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // AppUser currentUser = (AppUser) authentication.getPrincipal();
    }
}
