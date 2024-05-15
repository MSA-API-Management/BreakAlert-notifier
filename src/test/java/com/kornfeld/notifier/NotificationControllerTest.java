package com.kornfeld.notifier;

import com.kornfeld.notifier.controller.NotificationController;
import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.entities.DataChange;
import com.kornfeld.notifier.entities.Subscription;
import com.kornfeld.notifier.repositories.DataChangeRepository;
import com.kornfeld.notifier.repositories.SubscriptionRepository;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.EmailSender;
import com.kornfeld.notifier.services.Gpt35APIService;
import com.kornfeld.notifier.services.HTMLParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class NotificationControllerTest {
    @Mock
    private Gpt35APIService gpt3ApiService;

    @Mock
    private HTMLParser htmlParser;

    @Mock
    private EmailSender emailSender;

    @Mock
    private DataChangeRepository dataChangeRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationController controller;

    private Subscription subscription;
    private AppUser user;
    private DataChange dataChange;
    private ArgumentCaptor<DataChange> dataChangeCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        controller.setEmailSender(emailSender);
        controller.setRestTemplate(restTemplateBuilder);
    }


    private void testGenerateResponse() throws IOException {
        SubscriptionFoundSetUp();

        when(subscriptionRepository.findByAddressAndBranch(eq("testing.com"), eq("main"))).thenReturn(Optional.of(subscription));
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));
        when(htmlParser.extractChanges(anyString())).thenReturn("HTML Content");
        when(gpt3ApiService.generateResponse(anyString())).thenReturn("Response Content");
        doNothing().when(emailSender).sendEmail(anyString(), anyString(), anyString());
        doAnswer(invocation -> {
            DataChange savedDataChange = invocation.getArgument(0);
            assertEquals(dataChange.getTitle(), savedDataChange.getTitle());
            assertEquals(dataChange.getContent(), savedDataChange.getContent());
            return null;
        }).when(dataChangeRepository).save(any(DataChange.class));

        ResponseEntity<String> responseEntity = controller.generateResponse("HTML Content", "testing.com", "main", null);

        verify(subscriptionRepository, times(1)).findByAddressAndBranch("testing.com", "main");
        verify(htmlParser, times(1)).extractChanges("HTML Content");
        verify(gpt3ApiService, times(1)).generateResponse(anyString());
        verify(emailSender, times(1)).sendEmail("user@test.com", "Change(s) in testname detected", "Overview:\n\nResponse Content\n\n\nDetailed Changes:\nHTML Content\n\nSubscription Details:\n(address = testing.com, branch = main)");
        verify(dataChangeRepository, times(1)).save(dataChangeCaptor.capture());

        DataChange savedDataChange = dataChangeCaptor.getValue();

        assertEquals(dataChange.getTitle(), savedDataChange.getTitle());
        assertEquals(dataChange.getContent(), savedDataChange.getContent());
        assertEquals("API Change was successfully processed.", responseEntity.getBody());
    }

    @Test
    public void testGenerateResponse_SubscriptionFound_Successful() throws IOException {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(new ResponseEntity<String>("testBody", HttpStatus.OK));

        testGenerateResponse();
    }

    @Test
    public void testGenerateResponse_SubscriptionFound_NotSuccessful() throws IOException {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(new ResponseEntity<String>("testBody", HttpStatus.BAD_GATEWAY));

        testGenerateResponse();
    }

    @Test
    public void testGenerateResponse_SubscriptionFound_Error() throws IOException {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(null);

        testGenerateResponse();
    }

    @Test
    public void testGenerateResponse_SubscriptionFound_Exception() throws IOException {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenThrow(RestClientException.class);

        testGenerateResponse();
    }

    @Test
    public void testGenerateResponse_SubscriptionNotFound() throws IOException {
        when(subscriptionRepository.findByAddressAndBranch(anyString(), anyString())).thenReturn(Optional.empty());
        ResponseEntity<String> responseEntity = controller.generateResponse("<html><body><h1>test</h1></body></html>", "testing.com", "main", null);
        verify(subscriptionRepository, times(1)).findByAddressAndBranch(anyString(), anyString());

        assertEquals("API Change was successfully processed.", responseEntity.getBody());
    }

    private void SubscriptionFoundSetUp(){
        subscription = new Subscription();
        subscription.setAddress("testing.com");
        subscription.setBranch("main");
        subscription.setSubName("testname");

        user = new AppUser();
        user.setEmail("user@test.com");
        user.setToken("http://token.com");
        user.getSubscriptions().add(subscription);

        dataChange = new DataChange();
        dataChange.setTitle("Change(s) in testname detected");
        dataChange.setContent("Overview:\n\nResponse Content\n\n\nDetailed Changes:\nHTML Content\n\nSubscription Details:\n(address = testing.com, branch = main)");
        dataChangeCaptor = ArgumentCaptor.forClass(DataChange.class);
    }

}
