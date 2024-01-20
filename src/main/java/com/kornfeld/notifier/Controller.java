package com.kornfeld.notifier;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// create RESTAPI with RestController
@RestController
public class Controller {
    @GetMapping(path="/hello")
    public String helloWorld(){
        return "Hello World";
    }

}
