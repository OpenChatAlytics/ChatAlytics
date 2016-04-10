package com.chatalytics.compute.db.dao;

import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.model.ChatEntity;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link IEntityDAO}
 * @author giannis
 *
 */
public class EntityDAOImplTest {

    private EntityDAOImpl underTest;
    private DateTime mentionDate;

    @Before
    public void setUp() throws Exception {
        ChatAlyticsConfig config = new ChatAlyticsConfig();
        underTest = new EntityDAOImpl(config);
        underTest.startAsync().awaitRunning();

        mentionDate = new DateTime().withZone(DateTimeZone.UTC);

        // Insert a bunch of test values
        underTest.persistEntity(new ChatEntity("entity1", 1, mentionDate, "giannis", "room1"));
        underTest.persistEntity(new ChatEntity("entity2", 1, mentionDate, "giannis", "room1"));
        underTest.persistEntity(new ChatEntity("entity1", 1, mentionDate, "giannis", "room2"));
        underTest.persistEntity(new ChatEntity("entity1", 1, mentionDate, "jane", "room1"));
    }

    /**
     * Makes sure that the correct entity occurrence sums are returned.
     */
    @Test
    public void testGetTotalMentionsForEntity() {

        Interval timeInterval = new Interval(mentionDate, mentionDate.plusHours(3));

        // Make sure that 0 is returned when nothing is found
        assertEquals(0, underTest.getTotalMentionsForEntity("unknownentity",
                                                            timeInterval,
                                                            Optional.absent(),
                                                            Optional.absent()));

        // make sure that the sums are correct for a bunch of different queries
        int result = underTest.getTotalMentionsForEntity("entity1", timeInterval,
                                                          Optional.absent(), Optional.absent());
        assertEquals(3, result);

        result = underTest.getTotalMentionsForEntity("entity1", timeInterval,
                                                     Optional.of("room1"), Optional.absent());
        assertEquals(2, result);

        result = underTest.getTotalMentionsForEntity("entity1", timeInterval,
                                                     Optional.of("room1"), Optional.of("giannis"));
        assertEquals(1, result);
    }

    /**
     * Makes sure that the correct amount of entity mentions are returned
     */
    @Test
    public void testGetAllMentionsForEntity() {
        Interval timeInterval = new Interval(mentionDate, mentionDate.plusHours(3));
        List<ChatEntity> result = underTest.getAllMentionsForEntity("entity1", timeInterval,
                                                                    Optional.absent(),
                                                                    Optional.absent());
        assertEquals(3, result.size());

        result = underTest.getAllMentionsForEntity("entity1", timeInterval, Optional.of("room1"),
                                                Optional.absent());
        assertEquals(2, result.size());

        result = underTest.getAllMentionsForEntity("entity1", timeInterval, Optional.of("room1"),
                                                Optional.of("giannis"));
        assertEquals(1, result.size());
    }

    @Test
    public void testGetTopEntities() {
        Interval timeInterval = new Interval(mentionDate, mentionDate.plusHours(3));
        Map<String, Long> result =
            underTest.getTopEntities(timeInterval, Optional.absent(), Optional.absent(), 10);
        assertEquals(2, result.size());
        assertEquals(3L, result.get("entity1").longValue());
        assertEquals(1L, result.get("entity2").longValue());

        result = underTest.getTopEntities(timeInterval, Optional.of("room1"), Optional.absent(), 10);
        assertEquals(2, result.size());
        assertEquals(2L, result.get("entity1").longValue());
        assertEquals(1L, result.get("entity2").longValue());

        result = underTest.getTopEntities(timeInterval, Optional.of("room1"),
                                          Optional.of("giannis"), 10);
        assertEquals(2, result.size());
        assertEquals(1L, result.get("entity1").longValue());
        assertEquals(1L, result.get("entity2").longValue());
    }

    @After
    public void tearDown() throws Exception {
        underTest.stopAsync().awaitTerminated();
    }
}
