package com.kornfeld.notifier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private DataChangeRepository dataChangeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    public SubscriptionController(DataChangeRepository dataChangeRepository, SubscriptionRepository subscriptionRepository, UserRepository userRepository) {
        this.dataChangeRepository = dataChangeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
    }


    // To test in windows, call with:
    // ...\Projekt\notifier> curl -X POST -H "Content-Type: application/json" -d "[{\"subName\": \"Subscription1\", \"address\": \"http.com\", \"branch\": \"main\"}, {\"subName\": \"Subscription2\", \"address\": \"http.test.com\", \"branch\": \"main\"}]" http://localhost:8080/subscriptions/processAll?webUrl=https://gitlab.com/api/v4/projects/53346840/ref/main/trigger/pipeline?token=glptt-5f5619568b281e261c8cc546bdd22be10f001df9
    @PostMapping("/processAll")
    @Transactional
    public ResponseEntity<String> processJson(
            @RequestBody List<Subscription> subscriptionList,
            @RequestParam(value = "webUrl") String webUrl) {

        System.out.println("---------- Processing Subscription Objects with webUrl: " + webUrl + " ----------");

        List<AppUser> allUsers = userRepository.findAll();

        boolean userExists = false;

        for (AppUser user : allUsers) {

            if(webUrl.equals(user.getToken())){
                userExists = true;
                entityManager.persist(user);
                Set<Subscription> allPrevSubscriptions = user.getSubscriptions();

                for (Subscription sub : allPrevSubscriptions) {
                    System.out.println("SUB ID: " + sub.getId());
                }
                System.out.println("---- After Deletion: ");
                user.getSubscriptions().removeIf(prevSub ->
                        subscriptionList.stream()
                                .noneMatch(newSub ->
                                        newSub.getAddress().equals(prevSub.getAddress()) &&
                                                newSub.getBranch().equals(prevSub.getBranch())));
                for (Subscription sub : allPrevSubscriptions) {
                    System.out.println("SUB ID: " + sub.getId());
                }

                for (Subscription newSubscription : subscriptionList) {
                    boolean exists = false;

                    System.out.println("-- Received Subscription: " + newSubscription.getSubName() + " - " + newSubscription.getBranch() + " - " +newSubscription.getAddress() + " --");

                    for (Subscription prevSub : allPrevSubscriptions) {
                        if(prevSub.getAddress() != null && prevSub.getBranch() != null) {
                            if (prevSub.getAddress().equals(newSubscription.getAddress()) && prevSub.getBranch().equals(newSubscription.getBranch())) {
                                System.out.println("This subscription already exists for user "+ user.getName() +" with address: " + newSubscription.getAddress() + ", branch: " + newSubscription.getBranch() + " under the name '" + prevSub.getSubName() + "' " +  prevSub.getId());
                                exists = true;
                                break;
                            }
                        }
                    }

                    if(!exists){
                        System.out.println("User has no subscription to it up to now");
                        setSubscription(newSubscription, user);
                    }
                }
                System.out.println("All SUB IDs after changes: ");
                for (Subscription sub : allPrevSubscriptions) {
                    System.out.println("SUB ID: " + sub.getId());
                }
                break;
            }
        }

        if(!userExists){
            System.out.println("User does not exist in DB. So user gets created without name, password and email.");

            AppUser user = new AppUser();
            user.setToken(webUrl);
            entityManager.persist(user);
            userRepository.findAll().add(user);

            System.out.println("User with token '" + webUrl + "' successfully created.");

            for (Subscription newSubscription : subscriptionList) {
                setSubscription(newSubscription, user);
            }
        }

        return ResponseEntity.ok("JSON-Data processed");
    }


    private void setSubscription(Subscription newSubscription, AppUser user){
        Optional<Subscription> subscription = subscriptionRepository.findByAddressAndBranch(newSubscription.getAddress(), newSubscription.getBranch());
        if(!subscription.isPresent()){
            System.out.println(newSubscription.getSubName() + " does not exist in DB already. So it gets created.");
            subscriptionRepository.save(newSubscription);
            System.out.println("Created " + newSubscription.getSubName() + " " +newSubscription.getId() + " successfully.");
            user.getSubscriptions().add(newSubscription);
            System.out.println("Created a connection from the user to the subscription.");
        }else{
            System.out.println(newSubscription.getSubName() + " " + subscription.get().getId() + " already exists in DB with name " + subscription.get().getSubName() + " and ID " + subscription.get().getId());
            user.getSubscriptions().add(subscription.get());
            System.out.println("Created a connection from the user to the subscription.");
        }
    }

}


