package com.kornfeld.notifier;


import com.kornfeld.notifier.services.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JWTServiceTest {

    private JWTService jwtService;
    private String generatedToken;

    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "password";
    private static final String SECRET_KEY = "62f525b769ee2345e560c41ad8d3dc27e5379dfb4049fa0fe87e537d5a4fa72c";

    @BeforeEach
    public void setUp() {
        jwtService = new JWTService();
        jwtService.setExpiration(3600000);
        jwtService.setSecretKey(SECRET_KEY);

        UserDetails userDetails = new User(USERNAME, PASSWORD, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        generatedToken = jwtService.generateToken(userDetails);
    }

    @Test
    public void testGenerateToken() {
        assertEquals(USERNAME, jwtService.getUsername(generatedToken));
    }

    @Test
    public void testIsTokenTimeValid() {
        assertTrue(jwtService.isTokenTimeValid(generatedToken));
    }



}
