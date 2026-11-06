package net.jamnig.breakalert.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class ErrorController implements org.springframework.boot.webmvc.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            log.warn("Application encountered an HTTP error. Status code: {}", statusCode);

            if (statusCode == 403) {
                log.warn("User attempted to access a forbidden resource.");
                // return "error/403";
            } else if (statusCode == 404) {
                // return "error/404";
            }
        } else {
            log.error("Application encountered an unknown internal error.");
        }

        return "error";
    }
}
