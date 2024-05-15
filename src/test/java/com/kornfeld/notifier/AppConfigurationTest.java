package com.kornfeld.notifier;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.security.AppConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AppConfigurationTest {

    @Autowired
    private UserDetailsService userDetailsService;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private AppConfiguration appConfiguration;

    private AppUser user;
    private static final String USERNAME = "testUser";
    private static final String EMAIL = "testUser@gmail.com";
    private static final String PASSWORD = "password";

    @Test
    public void testUserDetailsService() {
        user = new AppUser();
        setUpUser();

        when(userRepository.findByName("testUser")).thenReturn(user);

        AppConfiguration appConfiguration = new AppConfiguration(userRepository);
        UserDetailsService userDetailsService = appConfiguration.userDetailsService();

        assertNotNull(userDetailsService.loadUserByUsername("testUser"));
    }

    @Test
    public void testUserDetailsService_Error() {
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nonExistingUser");
        });
    }


    public void setUpUser(){
        user.setId(1L);
        user.setName(USERNAME);
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);
    }

}
