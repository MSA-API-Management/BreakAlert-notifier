package com.kornfeld.notifier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private DataChangeRepository dataChangeRepository;

    private RestTemplate restTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public SubscriptionController(DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }


    // To test in windows, call with:
    // ...\Projekt\notifier> curl -X POST -H "Content-Type: application/json" -d "[{\"subName\": \"Subscription1\", \"address\": \"http.com\"}, {\"subName\": \"Subscription2\", \"address\": \"http.test.com\"}]" http://localhost:8080/subscriptions/processAll
    @PostMapping("/processAll")
    public ResponseEntity<String> processJson(@RequestBody List<Subscription> subscriptionList) {
        System.out.println("Processing Subscription Objects...");

        List<Subscription> allPrevSubscriptions = subscriptionRepository.findAll();

        for (Subscription newSubscription : subscriptionList) {
            int checkForExistence = 0;

            System.out.println("Received Subscription: " + newSubscription.getSubName() + " " + newSubscription.getAddress());

            for (Subscription prevSub : allPrevSubscriptions) {
                if(prevSub.getSubName() == null || prevSub.getAddress() == null) {
                    allPrevSubscriptions.remove(prevSub);
                }

                if(prevSub.getAddress().equals(newSubscription.getAddress())){
                    System.out.println(newSubscription.getSubName() + " already exists with address: " + newSubscription.getAddress());
                    checkForExistence = 1;
                    break;
                }
            }

            if(checkForExistence == 0 && !allPrevSubscriptions.isEmpty()){
                System.out.println(newSubscription.getSubName() + " does not exist. So it gets created.");
                subscriptionRepository.save(newSubscription);
                System.out.println("Created " + newSubscription.getSubName() + " successfully.");
            }
            // ID of subscription is NOT allowed to change
        }

        return ResponseEntity.ok("JSON-Data processed");
    }
}