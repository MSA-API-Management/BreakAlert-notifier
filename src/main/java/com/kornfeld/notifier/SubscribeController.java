package com.kornfeld.notifier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SubscribeController {


    @PostMapping("/subscribeToAPI")
    public String processRegister(Model model) {
        return "index";
    }

}
