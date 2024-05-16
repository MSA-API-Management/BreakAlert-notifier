package com.kornfeld.notifier;

import com.kornfeld.notifier.controller.ErrorController;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorControllerTest {
    @InjectMocks
    ErrorController errorController;

    @Test
    public void testHandleError() {
        MockitoAnnotations.initMocks(this);

        MockHttpServletRequest request = new MockHttpServletRequest();
        String result = errorController.handleError(request);

        assertEquals("error", result);
    }

}
