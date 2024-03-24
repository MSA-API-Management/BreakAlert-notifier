package com.kornfeld.notifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.*;

@Controller
public class DashboardController {
    private final UserRepository userRepository;
    private final DataChangeRepository dataChangeRepository;
    private final SubscriptionRepository subscriptionRepository;
    @PersistenceContext
    private EntityManager entityManager;
    private RestTemplate restTemplate;

    @Autowired
    public DashboardController(UserRepository userRepository, DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository, RestTemplateBuilder restTemplate) {
        this.userRepository = userRepository;
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.restTemplate = restTemplate.build();
    }
    @GetMapping("/dashboard")
    public String showDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        System.out.println("Redirecter");

        //String username = userDetails.getUsername();
        //System.out.println("Current Username: "+username);

        /*Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        System.out.println("Redirecter: UN = " + username);
        model.addAttribute("username", username);*/

        model.addAttribute("username", "Benutzer1");

        //System.out.println(userRepository.selectAllUsers().toString());
        AppUser appUser = getCurrentUser();
        Set<Subscription> subscriptions = appUser.getSubscriptions();

        List<Subscription> toSubscribe = subscriptionRepository.findAll();
        toSubscribe.removeAll(subscriptions);
        model.addAttribute("toSubscribe", toSubscribe);

        if(appUser.getToken()!=null || !appUser.getToken().isEmpty()){
            model.addAttribute("tokenExists", true);
        }

        Map<Long, Set<DataChange>> dataChangesMap = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            System.out.println("Subscription ID"+subscription.getId());
            Set<DataChange> dataChanges = dataChangeRepository.findBySubscriptionId(subscription.getId());
            if(dataChanges!=null) {
                dataChangesMap.put(subscription.getId(), dataChanges);
            }
        }
        System.out.println("DataChanges Map: " + dataChangesMap);
        model.addAttribute("subscriptions", subscriptions);

        model.addAttribute("dataChangesMap", dataChangesMap);

        //String username = principal.getName();
        //model.addAttribute("username", userDetails.getUsername());

        return "dashboard";
    }

    @PostMapping("/subscribe")
    @Transactional
    public String subscribe(@RequestParam Long subscriptionId, Model model) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        AppUser user = getCurrentUser();
        if (subscription != null) {
            user.getSubscriptions().add(subscription);
            model.addAttribute("subscriptionSuccess", true);
        } else {
            model.addAttribute("subscriptionError", true);
        }

        Set<Subscription> subscriptions = user.getSubscriptions();
        List<Subscription> toSubscribe = subscriptionRepository.findAll();
        toSubscribe.removeAll(subscriptions);
        model.addAttribute("toSubscribe", toSubscribe);

        return "redirect:/dashboard";
    }

    @PostMapping("/saveToken")
    @Transactional
    public String saveToken(@RequestParam String tokenLink) {

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
            System.out.println("webUrl: " + tokenLink);
            postResponse = restTemplate.postForEntity(tokenLink, entity, String.class);
            if (postResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("POST request successful. Response: " + postResponse.getBody());
                gitLabAvailable = true;
            } else {
                System.out.println("POST request failed. Response: " + postResponse.getBody());
            }
        } catch (RestClientException e) {
            System.out.println("Error while making POST request: " + e.getMessage());
        }

        if(gitLabAvailable){
            AppUser user = getCurrentUser();
            entityManager.persist(user);
            user.setToken(tokenLink);
            userRepository.save(user);
        }

        return "redirect:/dashboard";
    }


    @PostMapping("/dashboard/deleteSubscription/{subscriptionId}")
    @Transactional
    public String deleteSubscription(@PathVariable Long subscriptionId, Model model) {
        Optional<Subscription> subscription = subscriptionRepository.findById(subscriptionId);
        AppUser user = getCurrentUser();
        entityManager.persist(user);

        if(subscription.isPresent()){
            user.getSubscriptions().remove(subscription.get());
            System.out.println("Successfully deleted the subscription");
        }else{
            System.out.println("Error when deleting the subscription");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/changeSubscriptionName/{subscriptionId}")
    @Transactional
    public String changeSubscriptionName(@PathVariable Long subscriptionId, @RequestParam String newName, Model model) {
        Optional<Subscription> subscriptionOptional = subscriptionRepository.findById(subscriptionId);

        if (subscriptionOptional.isPresent()) {
            Subscription subscription = subscriptionOptional.get();
            subscription.setSubName(newName);
            subscriptionRepository.save(subscription);
            System.out.println("Successfully changed the name of the subscription");
        } else {
            System.out.println("Error when changing the name of the subscription");
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/logout")
    public String logout( Model model) {
        return "index";
    }

    public AppUser getCurrentUser(){
        return userRepository.findByName("Benutzer1");
    }

}
