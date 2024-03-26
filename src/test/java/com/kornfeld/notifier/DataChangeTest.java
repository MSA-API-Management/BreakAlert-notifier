package com.kornfeld.notifier;

import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class DataChangeTest {
    private static DataChange dataChange;
    private static final String TITLE = "Some Title";
    private static final String CONTENT = "Some content";
    private static final LocalDateTime TIMESTAMP = LocalDateTime.now();

    @BeforeAll
    public static void setUp() {
        dataChange = new DataChange();

        dataChange.setId(1L);
        dataChange.setTitle(TITLE);
        dataChange.setContent(CONTENT);
        dataChange.setTimestamp(TIMESTAMP);

        Subscription subscription = new Subscription();
        subscription.setId(1L);
        dataChange.setSubscription(subscription);
    }

    @Test
    public void testIdProperty() {
        assertEquals(1L, dataChange.getId());
    }

    @Test
    public void testTitleProperty() {
        assertEquals(TITLE, dataChange.getTitle());
    }

    @Test
    public void testContentProperty() {
        assertEquals(CONTENT, dataChange.getContent());
    }

    @Test
    public void testSubscriptionProperty() {
        assertNotNull(dataChange.getSubscription());
        assertEquals(1L, dataChange.getSubscription().getId());
    }

    @Test
    public void testTimestampProperty() {
        assertEquals(TIMESTAMP, dataChange.getTimestamp());
    }
}

