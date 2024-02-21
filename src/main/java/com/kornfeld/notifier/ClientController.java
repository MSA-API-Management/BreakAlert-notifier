package com.kornfeld.notifier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ClientController {
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ClientController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/setClient")
    @Transactional
    public ResponseEntity<String> setClient(@RequestParam int isClient, @RequestParam String token) {
        List<AppUser> user = userRepository.findByToken(token);
        if(user.size() == 1){
            System.out.println("Setting user as client from gitlab.");
            entityManager.persist(user.get(0));
            user.get(0).setClient(isClient > 0);
            System.out.println("Setting was successful");
            return ResponseEntity.ok("Setting the user as a client / not a client was successful.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No or multiple users found with the given token.");
        }

    }
}
