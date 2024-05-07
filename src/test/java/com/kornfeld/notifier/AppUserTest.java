package com.kornfeld.notifier;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.Subscription;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class AppUserTest {
    private static AppUser appUser;
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "123";
    private static final String EMAIL = "user@test.com";
    private static final String TOKEN = "http://token.com";

    @BeforeAll
    public static void setUp() {
        appUser = new AppUser();

        appUser.setId(1L);
        appUser.setName(USERNAME);
        appUser.setPassword(PASSWORD);
        appUser.setEmail(EMAIL);
        appUser.setToken(TOKEN);

        Subscription subscription = mock(Subscription.class);
        appUser.getSubscriptions().add(subscription);
    }

    @Test
    public void testIdProperty() {
        assertEquals(1L, appUser.getId());
    }

    @Test
    public void testNameProperty() {
        assertEquals(USERNAME, appUser.getName());
    }

    @Test
    public void testPasswordProperty() {
        assertEquals(PASSWORD, appUser.getPassword());
    }

    @Test
    public void testEmailProperty() {
        assertEquals(EMAIL, appUser.getEmail());
    }

    @Test
    public void testTokenProperty() {
        assertEquals(TOKEN, appUser.getToken());
    }

    @Test
    public void testSubscriptionsProperty() {
        assertNotNull(appUser.getSubscriptions());
        assertEquals(1, appUser.getSubscriptions().size());
    }

    @Test
    public void testGetAuthorities() {
        assertTrue(appUser.getAuthorities().isEmpty());
    }

    @Test
    public void testGetUsername() {
        assertEquals(USERNAME, appUser.getUsername());
    }

    @Test
    public void testIsAccountNonExpired() {
        assertTrue(appUser.isAccountNonExpired());
    }

    @Test
    public void testIsAccountNonLocked() {
        assertTrue(appUser.isAccountNonLocked());
    }

    @Test
    public void testIsCredentialsNonExpired() {
        assertTrue(appUser.isCredentialsNonExpired());
    }

    @Test
    public void testIsEnabled() {
        assertTrue(appUser.isEnabled());
    }

}
