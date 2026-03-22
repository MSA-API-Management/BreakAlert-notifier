package com.kornfeld.notifier.controller;

import com.kornfeld.notifier.entities.AppUser;
import com.kornfeld.notifier.repositories.UserRepository;
import com.kornfeld.notifier.services.AuthenticationService;
import com.kornfeld.notifier.services.JWTService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final AuthenticationService authenticationService;

    @Autowired
    public LoginController(UserRepository userRepository, JWTService jwtService, AuthenticationService authenticationService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("")
    public String viewHomePage() {
        return "index";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new AppUser());
        return "signup";
    }

    @PostMapping("/process_register")
    public String processRegister(AppUser user, Model model) {
        List<AppUser> list = userRepository.findByNameOrEmail(user.getName(), user.getEmail());
        for (AppUser user1: list) {
            System.out.println("REGISTER - Users already in Database " + user1.getName()+", " + user1.getEmail());
        }
        if(list.isEmpty()){
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);

            userRepository.save(user);
            model.addAttribute("successMessage", "Registration successful! You can now log in.");
        }
        else {
            model.addAttribute("errorMessage", "User already exists. Please choose a different username or email.");
            System.out.println("REGISTER - User already exists");
        }
        return "index";
    }

    @GetMapping("/loginPage")
    public String showLoginPage() {
        return "loginPage";
    }

    @PostMapping("/loginPage")
    public String processLogin(@RequestParam("username") String username, @RequestParam("password") String password, HttpServletResponse response, Model model) {
        try {
            AppUser authenticatedUser = authenticationService.authenticate(username, password);
            if (authenticatedUser != null) {
                String jwtToken = jwtService.generateToken(authenticatedUser);

                Cookie jwtCookie = new Cookie("jwt", jwtToken);
                jwtCookie.setHttpOnly(true);
                jwtCookie.setSecure(false);
                jwtCookie.setPath("/");
                jwtCookie.setMaxAge(jwtService.getExpiration());

                response.addCookie(jwtCookie);

                return "redirect:/dashboard";
            } else {
                return "index";
            }
        } catch (Exception e){
            model.addAttribute("credentials",true);
            return "error";
        }
    }

}
