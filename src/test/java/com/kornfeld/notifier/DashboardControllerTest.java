package com.kornfeld.notifier;

import com.kornfeld.notifier.controller.DashboardController;
import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.Subscription;
import com.kornfeld.notifier.repositories.DataChangeRepository;
import com.kornfeld.notifier.repositories.SubscriptionRepository;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.JWTService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DashboardControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DataChangeRepository dataChangeRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private JWTService jwtService;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DashboardController dashboardController;

    private Model model;
    private AppUser user;
    private Subscription subscription;
    private RedirectAttributes redirectAttributes;

    private static final String USERNAME = "testUser";
    private static final String EMAIL = "testUser@gmail.com";
    private static final String PASSWORD = "password";
    private static final String JWT_TOKEN = "testToken";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        model = mock(Model.class);
        redirectAttributes = mock(RedirectAttributes.class);

        user = new AppUser();
        subscription = new Subscription();
        setUpUser();

        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        dashboardController.setRestTemplate(restTemplateBuilder);

        dashboardController.setEntityManager(entityManager);

        // Always verify the user
        when(jwtService.isTokenTimeValid(JWT_TOKEN)).thenReturn(true);
        when(jwtService.getUsername(JWT_TOKEN)).thenReturn(USERNAME);
        when(userRepository.findByName(USERNAME)).thenReturn(user);
    }

    @Test
    public void testShowDashboardNoStoredToken() {
        when(dataChangeRepository.findBySubscriptionId(anyLong())).thenReturn(new HashSet<>());

        String returnVal = dashboardController.showDashboard(model, JWT_TOKEN);

        assertEquals("dashboard", returnVal);

        verify(model).addAttribute(eq("username"), eq(USERNAME));
        verify(model).addAttribute(eq("toSubscribe"), anyList());
        verify(model).addAttribute(eq("subscriptions"), anySet());
        verify(model).addAttribute(eq("dataChangesMap"), anyMap());
        verify(dataChangeRepository).findBySubscriptionId(1L);
    }

    @Test
    public void testShowDashboardStoredToken() {
        user.setToken("testToken");
        when(dataChangeRepository.findBySubscriptionId(anyLong())).thenReturn(new HashSet<>());

        String returnVal = dashboardController.showDashboard(model, JWT_TOKEN);

        assertEquals("dashboard", returnVal);

        verify(model).addAttribute(eq("username"), eq(USERNAME));
        verify(model).addAttribute(eq("toSubscribe"), anyList());
        verify(model).addAttribute(eq("subscriptions"), anySet());
        verify(model).addAttribute(eq("dataChangesMap"), anyMap());
        verify(model).addAttribute(eq("tokenExists"), eq(true));
        verify(dataChangeRepository).findBySubscriptionId(1L);
    }

    @Test
    public void testShowDashboardNoDataChange() {
        when(dataChangeRepository.findBySubscriptionId(anyLong())).thenReturn(null);

        String returnVal = dashboardController.showDashboard(model, JWT_TOKEN);

        assertEquals("dashboard", returnVal);

        verify(model).addAttribute(eq("username"), eq(USERNAME));
        verify(model).addAttribute(eq("toSubscribe"), anyList());
        verify(model).addAttribute(eq("subscriptions"), anySet());
        verify(dataChangeRepository).findBySubscriptionId(1L);
    }

    @Test
    public void testSubscribe() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(new Subscription()));

        String returnVal = dashboardController.subscribe(1L, JWT_TOKEN, model, redirectAttributes);

        assertEquals("redirect:/dashboard", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
        verify(model).addAttribute(eq("subscriptionSuccess"), eq(true));
        verify(model).addAttribute(eq("toSubscribe"), anyList());
    }

    @Test
    public void testSubscribeError() {
        String returnVal = dashboardController.subscribe(200L, JWT_TOKEN, model, redirectAttributes);

        assertEquals("redirect:/dashboard", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
        verify(model).addAttribute(eq("subscriptionError"), eq(true));
        verify(model).addAttribute(eq("toSubscribe"), anyList());
    }

    @Test
    public void testSaveToken_Successful() {
        String response = "{\"title\":\"Initialize Client\",\"description\":\"Test\"}";
        ResponseEntity<String> postResponse = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(postResponse);

        String returnVal = dashboardController.saveToken("tokenLink", JWT_TOKEN, redirectAttributes, model);

        assertEquals("redirect:/clientConfirmation", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
        verify(model).addAttribute(eq("urlFalse"), eq(false));
        verify(userRepository).save(any());
    }
    @Test
    public void testSaveToken_NotSuccessful() {
        ResponseEntity<String> postResponse = new ResponseEntity<>("response", HttpStatus.BAD_GATEWAY);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(postResponse);

        String returnVal = dashboardController.saveToken("tokenLink", JWT_TOKEN, redirectAttributes, model);

        assertEquals("redirect:/clientConfirmation", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
        verify(model).addAttribute(eq("urlFalse"), eq(true));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testSaveToken_Error() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenThrow(RestClientException.class);

        String returnVal = dashboardController.saveToken("tokenLink", JWT_TOKEN, redirectAttributes, model);

        assertEquals("redirect:/clientConfirmation", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
        verify(model).addAttribute(eq("urlFalse"), eq(true));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testClientConfirmationWithGitLabAvailable() {
        boolean gitLabAvailable = true;
        String result = dashboardController.clientConfirmation(model, JWT_TOKEN, gitLabAvailable, redirectAttributes);

        assertEquals("confirmClient", result);

        verify(model).addAttribute("urlCorrect", true);
        verify(redirectAttributes).addAttribute("token", JWT_TOKEN);
    }

    @Test
    public void testClientConfirmationWithGitLabNotAvailable() {
        boolean gitLabAvailable = false;
        String result = dashboardController.clientConfirmation(model, JWT_TOKEN, gitLabAvailable, redirectAttributes);

        assertEquals("confirmClient", result);

        verify(model).addAttribute("urlWrong", true);
        verify(redirectAttributes).addAttribute("token", JWT_TOKEN);
    }

    @Test
    public void testDeleteSubscription() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(new Subscription()));

        String returnVal = dashboardController.deleteSubscription(1L, JWT_TOKEN, model, redirectAttributes);

        assertEquals("redirect:/dashboard", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
    }

    @Test
    public void testDeleteSubscriptionError() {
        String returnVal = dashboardController.deleteSubscription(200L, JWT_TOKEN, model, redirectAttributes);

        assertEquals("redirect:/dashboard", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
    }

    @Test
    public void testChangeSubscriptionName() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(new Subscription()));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(new Subscription());

        String returnVal = dashboardController.changeSubscriptionName(1L, "newName", JWT_TOKEN, model, redirectAttributes);

        assertEquals("redirect:/dashboard", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
        verify(subscriptionRepository).save(any());
    }

    @Test
    public void testChangeSubscriptionName_Error() {
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.empty());

        String returnVal = dashboardController.changeSubscriptionName(1L, "newName", JWT_TOKEN, model, redirectAttributes);

        assertEquals("redirect:/dashboard", returnVal);

        verify(redirectAttributes).addAttribute(eq("token"), eq(JWT_TOKEN));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    public void testLogout() {
        String returnVal = dashboardController.logout(model);

        assertEquals("index", returnVal);
    }


    public void setUpUser(){
        user.setId(1L);
        user.setName(USERNAME);
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);

        subscription.setId(1L);
        user.getSubscriptions().add(subscription);
    }


}
