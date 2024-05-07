package com.kornfeld.notifier;

import com.kornfeld.notifier.entities.DataChange;
import com.kornfeld.notifier.entities.Subscription;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class SubscriptionTest {
    private static Subscription subscription;
    private static final String SUB_NAME = "Test Subscription";
    private static final String ADDRESS = "Test Address";
    private static final String BRANCH = "Test Branch";

    @BeforeAll
    public static void setUp() {
        subscription = new Subscription();

        subscription.setId(1L);
        subscription.setSubName(SUB_NAME);
        subscription.setAddress(ADDRESS);
        subscription.setBranch(BRANCH);

        DataChange dataChange = mock(DataChange.class);
        subscription.getDataChanges().add(dataChange);
    }

    @Test
    public void testIdProperty() {
        assertEquals(1L, subscription.getId());
    }

    @Test
    public void testSubNameProperty() {
        assertEquals(SUB_NAME, subscription.getSubName());
    }

    @Test
    public void testAddressProperty() {
        assertEquals(ADDRESS, subscription.getAddress());
    }

    @Test
    public void testBranchProperty() {
        assertEquals(BRANCH, subscription.getBranch());
    }

    @Test
    public void testDataChangesProperty() {
        assertNotNull(subscription.getDataChanges());
        assertEquals(1, subscription.getDataChanges().size());
    }
}

