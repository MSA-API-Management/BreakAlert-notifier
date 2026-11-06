package com.kornfeld.notifier.controller;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.AuthenticationService;
import com.kornfeld.notifier.services.JWTService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final AuthenticationService authenticationService;

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/")
    public String viewHomePage() {
        return "index";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new AppUser());
        return "signup";
    }

    @PostMapping("/process_register")
    public String processRegister(AppUser user, RedirectAttributes redirectAttributes) {
        log.info("Attempting to register new user: {}", user.getName());

        List<AppUser> existingUsers = userRepository.findByNameOrEmail(user.getName(), user.getEmail());

        if (existingUsers.isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);

            log.info("User '{}' successfully registered.", user.getName());

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! You can now log in.");
            return "redirect:/login";
        } else {
            log.warn("Registration failed: User with name '{}' or email '{}' already exists.", user.getName(), user.getEmail());

            redirectAttributes.addFlashAttribute("errorMessage", "Error: User already exists. Please choose a different username or email.");
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               HttpServletResponse response,
                               RedirectAttributes redirectAttributes) {
        log.info("Login attempt for user: {}", username);

        try {
            AppUser authenticatedUser = authenticationService.authenticate(username, password);

            if (authenticatedUser != null) {
                String jwtToken = jwtService.generateToken(authenticatedUser);

                Cookie jwtCookie = new Cookie("jwt", jwtToken);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setSecure(false); // TODO true if https
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(jwtService.getExpiration());

                response.addCookie(jwtCookie);
                log.info("User '{}' successfully logged in.", username);

                return "redirect:/dashboard";
            } else {
                log.warn("Login failed: Authentication returned null for user '{}'", username);
                redirectAttributes.addFlashAttribute("errorMessage", "Error: Invalid username or password.");
                return "redirect:/login";
            }
        } catch (Exception e) {
            log.warn("Authentication failed for user '{}': {}", username, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Invalid username or password.");
            return "redirect:/login";
        }
    }
}
