package com.kornfeld.notifier;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepository userRepository, AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    public AppUser authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        return userRepository.findByName(username);
    }
}
