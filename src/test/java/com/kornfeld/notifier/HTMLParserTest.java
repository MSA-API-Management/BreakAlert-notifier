package com.kornfeld.notifier;

import com.kornfeld.notifier.services.HTMLParser;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
public class HTMLParserTest {
    @InjectMocks
    private HTMLParser htmlParser;

    @Test
    public void testExtractChanges() {
        String htmlContent = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Api Change Log</title><link rel=\"stylesheet\" href=\"http://deepoove.com/swagger-diff/stylesheets/demo.css\"></head><body><header><h1>Api Change Log</h1></header><div class=\"article\"><div><h2>What&#x27;s New</h2><hr><ol><li><span class=\"GET\">GET</span>/Test <span>null</span></li></ol></div><div><h2>What&#x27;s Deleted</h2><hr><ol></ol></div><div><h2>What&#x27;s Deprecated</h2><hr><ol></ol></div><div><h2>What&#x27;s Changed</h2><hr><ol></ol></div></div></body></html>";
        String expectedResult = "\nWhat's New:\nGET/Test null \n\nWhat's Deleted:\n\n\nWhat's Deprecated:\n\n\nWhat's Changed:\n";

        String result = htmlParser.extractChanges(htmlContent);
        assertEquals(expectedResult, result);
    }
}
