package com.chatalytics.compute.db.dao;

import com.chatalytics.core.config.ChatAlyticsConfig;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link ChatAlyticsDAOImpl}
 *
 * @author giannis
 *
 */
public class ChatAlyticsDAOImplTest {

    private IChatAlyticsDAO underTest;

    @Before
    public void setUp() throws Exception {
        ChatAlyticsConfig config = new ChatAlyticsConfig();
        config.persistenceUnitName = "chatalytics-db-test";
        underTest = new ChatAlyticsDAOImpl(config);
        underTest.startAsync().awaitRunning();
    }

    /**
     * Makes sure that the last message pull dates are correct
     */
    @Test
    public void testGetLastMessagePullTime() {
        DateTime expectedDate = new DateTime(0).withZone(DateTimeZone.UTC);
        assertEquals(expectedDate, underTest.getLastMessagePullTime());
        expectedDate = expectedDate.plusHours(1);
        underTest.setLastMessagePullTime(expectedDate);
        assertEquals(expectedDate, underTest.getLastMessagePullTime());
    }

    @After
    public void tearDown() throws Exception {
        underTest.stopAsync().awaitTerminated();
    }

}
