package com.kornfeld.notifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@Controller
public class DashboardController {
    private final UserRepository userRepository;
    private final DataChangeRepository dataChangeRepository;
    private final SubscriptionRepository subscriptionRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public DashboardController(UserRepository userRepository, DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
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
    public String subscribe(@RequestParam String subscriptionLink,@RequestParam String nameOfSub, Model model) {
        System.out.println("subscriptionLink: "+subscriptionLink);
        System.out.println("name: "+nameOfSub);

        Subscription newSubscription = new Subscription();
        newSubscription.setSubName(nameOfSub);
        newSubscription.setAddress(subscriptionLink);

        Subscription savedSubscription = subscriptionRepository.save(newSubscription);

        AppUser user = getCurrentUser();
        entityManager.persist(user);

        user.getSubscriptions().add(savedSubscription);

        // DO REAL SUBSCRIBING HERE

        if (savedSubscription != null) {
            model.addAttribute("subscriptionSuccess", true);
        }else{
            model.addAttribute("subscriptionError", true);
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
