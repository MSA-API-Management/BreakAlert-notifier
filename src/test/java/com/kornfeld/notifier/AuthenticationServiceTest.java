package com.kornfeld.notifier;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthenticationServiceTest {

    private AuthenticationService authenticationService;
    private UserRepository userRepository;
    private AuthenticationManager authenticationManager;

    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "password";

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        authenticationManager = mock(AuthenticationManager.class);
        authenticationService = new AuthenticationService(userRepository, authenticationManager);
    }

    @Test
    public void testAuthenticate() {
        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD))).thenReturn(null);

        AppUser expectedUser = createAppUser();
        when(userRepository.findByName(USERNAME)).thenReturn(expectedUser);

        AppUser authenticatedUser = authenticationService.authenticate(USERNAME, PASSWORD);

        assertEquals(USERNAME, authenticatedUser.getName());
    }

    private AppUser createAppUser(){
        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setName(USERNAME);
        return appUser;
    }

}
