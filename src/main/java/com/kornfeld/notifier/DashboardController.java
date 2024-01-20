package com.kornfeld.notifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@Controller
public class DashboardController {
    private final UserRepository userRepository;
    private final DataChangeRepository dataChangeRepository;
    private final SubscriptionRepository subscriptionRepository;

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
        AppUser appUser = userRepository.findByName("Benutzer1");
        Set<Subscription> subscriptions = appUser.getSubscriptions();


        Map<Long, Set<DataChange>> dataChangesMap = new HashMap<>();
        for (Subscription subscription : subscriptions) {
            System.out.println("Subscription ID"+subscription.getId());
            Set<DataChange> dataChanges = dataChangeRepository.findBySubscriptionId(subscription.getId());
            /*System.out.println(dataChanges.get(0).getTitle());
            System.out.println(dataChanges.get(0).getContent());
            System.out.println(dataChanges.get(0).getSubscription().getId());*/
            System.out.println("DataChanges for Subscription ID " + subscription.getId() + ": " + dataChanges);
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
    public String subscribe(@RequestParam String subscriptionLink,@RequestParam String nameOfSub, Model model) {
        System.out.println("subscriptionLink: "+subscriptionLink);
        System.out.println("name: "+nameOfSub);

        Subscription newSubscription = new Subscription();
        newSubscription.setSubName(nameOfSub);
        newSubscription.setAddress(subscriptionLink);

        Subscription savedSubscription = subscriptionRepository.save(newSubscription);

        // SUBSCRIBE HERE

        if (savedSubscription != null) {
            model.addAttribute("subscriptionSuccess", true);
        }else{
            model.addAttribute("subscriptionError", true);
        }
        return "redirect:/dashboard";
    }

}
