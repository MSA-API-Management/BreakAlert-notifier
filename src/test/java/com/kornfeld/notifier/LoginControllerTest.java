package com.kornfeld.notifier;

import com.kornfeld.notifier.controller.LoginController;
import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.AuthenticationService;
import com.kornfeld.notifier.services.JWTService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class LoginControllerTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private JWTService jwtService;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private LoginController loginController;

    private Model model;
    private AppUser user;
    private static final String USERNAME = "testUser";
    private static final String EMAIL = "testUser@gmail.com";
    private static final String PASSWORD = "password";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        model = mock(Model.class);
        user = new AppUser();
    }

    @Test
    public void testViewHomePage() {
        String returnVal = loginController.viewHomePage();

        assertEquals("index", returnVal);
    }

    @Test
    public void testShowRegistrationForm() {
        String returnVal = loginController.showRegistrationForm(model);

        assertEquals("signup", returnVal);

        verify(model).addAttribute(eq("user"), any());
    }

    @Test
    void testProcessRegister_Success() {
        setUpUser();

        when(userRepository.findByNameOrEmail(anyString(), anyString())).thenReturn(new ArrayList<>());

        String returnVal = loginController.processRegister(user, model);

        assertEquals("index", returnVal);

        verify(userRepository).save(user);
        verify(model).addAttribute(eq("successMessage"), anyString());
    }

    @Test
    void testProcessRegister_Error() {
        setUpUser();
        List<AppUser> existingUsers = new ArrayList<>();
        existingUsers.add(user);

        when(userRepository.findByNameOrEmail(anyString(), anyString())).thenReturn(existingUsers);

        String returnVal = loginController.processRegister(user, model);

        assertEquals("index", returnVal);

        verify(userRepository, never()).save(any());
        verify(model).addAttribute(eq("errorMessage"), anyString());
    }

    @Test
    void testShowLoginPage() {
        String returnVal = loginController.showLoginPage();

        assertEquals("loginPage", returnVal);
    }

    @Test
    void testProcessLogin_Authenticated() {
        setUpUser();
        user.setId(1L);
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        when(authenticationService.authenticate(USERNAME, PASSWORD)).thenReturn(user);

        String returnVal = loginController.processLogin(USERNAME, PASSWORD, redirectAttributes, null, model);

        assertEquals("redirect:/dashboard", returnVal);

        verify(jwtService).generateToken(user);
        verify(redirectAttributes).addAttribute(eq("token"), any());
    }

    @Test
    void testProcessLogin_NotAuthenticated() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        when(authenticationService.authenticate(USERNAME, PASSWORD)).thenReturn(null);

        String returnVal = loginController.processLogin(USERNAME, PASSWORD, redirectAttributes, null, model);

        assertEquals("index", returnVal);

        verify(jwtService, never()).generateToken(any());
        verify(redirectAttributes, never()).addAttribute(eq("token"), anyString());
    }

    public void setUpUser(){
        user.setName(USERNAME);
        user.setEmail(EMAIL);
        user.setPassword(PASSWORD);
    }

}
