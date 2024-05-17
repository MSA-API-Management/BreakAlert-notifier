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
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String showDashboard(Model model, @RequestParam("token") String jwtToken) {
        System.out.println("Redirecter");

        AppUser appUser = getCurrentUser(jwtToken);
        entityManager.persist(appUser);
        Set<Subscription> subscriptions = appUser.getSubscriptions();
        model.addAttribute("username", appUser.getName());

        List<Subscription> toSubscribe = subscriptionRepository.findAll();
        toSubscribe.removeAll(subscriptions);
        model.addAttribute("toSubscribe", toSubscribe);

        if(appUser.getToken()!=null){
            if(!appUser.getToken().isEmpty()) {
                model.addAttribute("tokenExists", true);
            }
        }

        Map<Long, List<DataChange>> dataChangesMap = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            System.out.println("Subscription ID"+subscription.getId());
            Set<DataChange> dataChanges = dataChangeRepository.findBySubscriptionId(subscription.getId());
            if(dataChanges!=null) {
                List<DataChange> dataChangeList = new ArrayList<>(dataChanges);
                dataChangeList.sort((dc1, dc2) -> dc2.getTimestamp().compareTo(dc1.getTimestamp()));
                dataChangesMap.put(subscription.getId(), dataChangeList);
            }
        }
        System.out.println("DataChanges Map: " + dataChangesMap);
        model.addAttribute("subscriptions", subscriptions);

        model.addAttribute("dataChangesMap", dataChangesMap);

        return "dashboard";
    }

    @PostMapping("/subscribe")
    @Transactional
    public String subscribe(@RequestParam Long subscriptionId, @RequestParam String jwtToken, Model model, RedirectAttributes redirectAttributes) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        AppUser user = getCurrentUser(jwtToken);
        entityManager.persist(user);

        System.out.println("User ID: "+user.getId()+" wants to subscribe to subID="+subscriptionId);

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

        redirectAttributes.addAttribute("token", jwtToken);
        return "redirect:/dashboard";
    }

    @PostMapping("/saveToken")
    @Transactional
    public String saveToken(@RequestParam String tokenLink, @RequestParam String jwtToken, RedirectAttributes redirectAttributes, Model model) {

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
                System.out.println("Test POST request successful. Response: " + postResponse.getBody());
                gitLabAvailable = true;
            } else {
                System.out.println("Test POST request failed. Response: " + postResponse.getBody());
                System.out.println("Try again later or enter a different URL.");
                model.addAttribute("urlFalse", true);
            }
        } catch (RestClientException e) {
            System.out.println("Error while making Test POST request: " + e.getMessage());
            System.out.println("Try again later or enter a different URL.");
            model.addAttribute("urlFalse", true);
        }

        if(gitLabAvailable){
            AppUser user = getCurrentUser(jwtToken);
            entityManager.persist(user);
            user.setToken(tokenLink);
            userRepository.save(user);
        }
        model.addAttribute("urlFalse", false);
        redirectAttributes.addAttribute("gitLabAvailable", gitLabAvailable);
        redirectAttributes.addAttribute("token", jwtToken);
        return "redirect:/clientConfirmation";
    }

    @GetMapping("/clientConfirmation")
    public String clientConfirmation(Model model, @RequestParam("token") String jwtToken, @RequestParam("gitLabAvailable") boolean gitLabAvailable, RedirectAttributes redirectAttributes) {
        if (gitLabAvailable){
            model.addAttribute("urlCorrect", true);
        } else {
            model.addAttribute("urlWrong", true);
        }
        redirectAttributes.addAttribute("token", jwtToken);
        return "confirmClient";
    }


    @PostMapping("/dashboard/deleteSubscription/{subscriptionId}")
    @Transactional
    public String deleteSubscription(@PathVariable Long subscriptionId, @RequestParam String jwtToken, Model model, RedirectAttributes redirectAttributes) {
        Optional<Subscription> subscription = subscriptionRepository.findById(subscriptionId);
        AppUser user = getCurrentUser(jwtToken);
        entityManager.persist(user);

        if(subscription.isPresent()){
            user.getSubscriptions().remove(subscription.get());
            System.out.println("Successfully deleted the subscription");
        }else{
            System.out.println("Error when deleting the subscription");
        }
        redirectAttributes.addAttribute("token", jwtToken);
        return "redirect:/dashboard";
    }

    @PostMapping("/dashboard/changeSubscriptionName/{subscriptionId}")
    @Transactional
    public String changeSubscriptionName(@PathVariable Long subscriptionId, @RequestParam String newName, @RequestParam String jwtToken, Model model, RedirectAttributes redirectAttributes) {
        AppUser user = getCurrentUser(jwtToken);
        if(user!=null) {
            Optional<Subscription> subscriptionOptional = subscriptionRepository.findById(subscriptionId);

            if (subscriptionOptional.isPresent()) {
                Subscription subscription = subscriptionOptional.get();
                subscription.setSubName(newName);
                subscriptionRepository.save(subscription);
                System.out.println("Successfully changed the name of the subscription");
            } else {
                System.out.println("Error when changing the name of the subscription");
            }
        }
        redirectAttributes.addAttribute("token", jwtToken);
        return "redirect:/dashboard";
    }

    @PostMapping("/logout")
    public String logout( Model model) {
        return "index";
    }

    private AppUser getCurrentUser(String jwtToken){
        boolean tokenTimeValid = jwtService.isTokenTimeValid(jwtToken);
        AppUser user = null;
        if(tokenTimeValid) {
            String username = jwtService.getUsername(jwtToken);
            user = userRepository.findByName(username);
            if(user!=null) {
                System.out.println("Valid Token");
                setAuthentication(user);
            } else {
                System.out.println("Token is not valid");
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
