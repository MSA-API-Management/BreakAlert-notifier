package com.kornfeld.notifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    @Autowired
    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
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
            System.out.println("Users already in Database " + user1.getName()+", " + user1.getEmail()+", " + user1.getPassword());
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
            System.out.println("User already exists");
        }
        return "index";
    }

    @GetMapping("/loginPage")
    public String showLoginPage() {
        return "loginPage";
    }

    @PostMapping("/loginPage")
    public String processLogin(String username, String password, Model model) {
        AppUser dbAppUser = userRepository.findByName(username);
        if (dbAppUser != null) {
            String dbPassword = dbAppUser.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            // ADMIN not encoded (Testing purposes)
            if(password.equals(dbPassword)){
                System.out.println("not encoded admin " + dbAppUser.getName());
                model.addAttribute("username", username);
                return "redirect:/dashboard";
            }
            // USER encoded
            else if (passwordEncoder.matches(password, dbPassword)) {
                System.out.println(dbAppUser.getName());
                model.addAttribute("username", username);
                return "redirect:/dashboard";
            }else{
                System.out.println("Failed to log in");
                model.addAttribute("error", "Invalid credentials");
                return "loginPage";
            }
        } else {
            System.out.println("Failed to log in");
            model.addAttribute("error", "Invalid credentials");
            return "loginPage";
        }
    }

}
