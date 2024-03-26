package com.kornfeld.notifier;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class SubscriptionControllerTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private DataChangeRepository dataChangeRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionController controller;

    private Subscription existingSubscription1;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller.setDataChangeRepository(dataChangeRepository);
        controller.setSubscriptionRepository(subscriptionRepository);
        controller.setUserRepository(userRepository);
        controller.setEntityManager(entityManager);
    }

    @Test
    public void testProcessJson_UserExists_UpdateSubscriptions() {
        AppUser existingUser = createExistingUserWithSubscriptions();
        List<Subscription> newSubscriptions = createNewSubscriptions();

        when(userRepository.findAll()).thenReturn(List.of(existingUser));
        when(subscriptionRepository.findByAddressAndBranch("http://example.com", "main")).thenReturn(java.util.Optional.of(existingSubscription1));
        when(subscriptionRepository.findByAddressAndBranch("http://notNew.com", "main")).thenReturn(java.util.Optional.of(existingSubscription1));

        ResponseEntity<String> responseEntity = controller.processJson(newSubscriptions, "existingToken");

        verify(userRepository, times(1)).findAll();
        verify(subscriptionRepository, times(2)).findByAddressAndBranch(anyString(), anyString());
        verify(entityManager, times(1)).persist(existingUser);

        assertEquals("JSON-Data processed", responseEntity.getBody());
    }

    @Test
    public void testProcessJson_UserDoesNotExist() {
        List<AppUser> existingUsers = new ArrayList<>();
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setAddress("address.com");
        subscription.setSubName("address");
        subscription.setBranch("main");
        subscriptions.add(subscription);

        when(userRepository.findAll()).thenReturn(existingUsers);

        ResponseEntity<String> responseEntity = controller.processJson(subscriptions, "nonExistingToken");

        assertEquals("JSON-Data processed", responseEntity.getBody());
    }

    private AppUser createExistingUserWithSubscriptions() {
        AppUser existingUser = new AppUser();
        existingUser.setToken("existingToken");

        existingSubscription1 = new Subscription();
        existingSubscription1.setId(1L);
        existingSubscription1.setAddress("http://example.com");
        existingSubscription1.setBranch("main");

        Subscription existingSubscription2 = new Subscription();
        existingSubscription2.setId(2L);
        existingSubscription2.setAddress("http://test.com");
        existingSubscription2.setBranch("dev");

        existingUser.getSubscriptions().add(existingSubscription1);
        existingUser.getSubscriptions().add(existingSubscription2);

        return existingUser;
    }

    private List<Subscription> createNewSubscriptions() {
        List<Subscription> newSubscriptions = new ArrayList<>();

        Subscription newSubscription1 = new Subscription();
        newSubscription1.setId(3L);
        newSubscription1.setAddress("http://example.com");
        newSubscription1.setBranch("main");

        Subscription newSubscription2 = new Subscription();
        newSubscription2.setId(4L);
        newSubscription2.setAddress("http://new.com");
        newSubscription2.setBranch("feature");

        Subscription newSubscription3 = new Subscription();
        newSubscription3.setId(5L);
        newSubscription3.setAddress("http://notNew.com");
        newSubscription3.setBranch("main");

        newSubscriptions.add(newSubscription1);
        newSubscriptions.add(newSubscription2);
        newSubscriptions.add(newSubscription3);

        return newSubscriptions;
    }
}



