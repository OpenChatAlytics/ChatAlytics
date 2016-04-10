package com.chatalytics.compute.storm.bolt;

import com.chatalytics.compute.config.ConfigurationConstants;
import com.chatalytics.core.model.ChatEntity;
import com.chatalytics.core.model.FatMessage;
import com.chatalytics.core.model.Message;
import com.chatalytics.core.model.Room;
import com.chatalytics.core.model.User;
import com.google.common.collect.Maps;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link EntityExtractionBolt}.
 *
 * @author giannis
 *
 */
public class EntityExtractionBoltTest {

    private EntityExtractionBolt underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new EntityExtractionBolt();
        Map<String, String> confMap = Maps.newHashMapWithExpectedSize(1);
        confMap.put(ConfigurationConstants.CHATALYTICS_CONFIG.txt,
                        "apiRetries: 0\n" +
                        "persistenceUnitName: 'chatalytics-db-test'");
        underTest.prepare(confMap, mock(TopologyContext.class), mock(OutputCollector.class));
    }

    /**
     * Ensures that entities are properly extracted and returned from a {@link Message}.
     */
    @Test
    public void testExtractEntities() {
        DateTime date = DateTime.now().withZone(DateTimeZone.UTC);
        String mentionName = "jane";
        String userId = "1";
        String roomId = "100";
        String ent1 = "Jane Doe";
        String ent2 = "Mount Everest";
        Message msg = new Message(date, mentionName, userId,
                                  String.format("Today, %s is going to climb %s", ent1, ent2),
                                  roomId);
        User mockUser = mock(User.class);
        when(mockUser.getMentionName()).thenReturn("jane");
        Room mockRoom = mock(Room.class);
        when(mockRoom.getName()).thenReturn("theroom");
        FatMessage fatMessage = new FatMessage(msg, mockUser, mockRoom);
        List<ChatEntity> entities = underTest.extractEntities(fatMessage);
        assertEquals(2, entities.size());

        ChatEntity entity = entities.get(1);
        assertEquals(ent1, entity.getEntityValue());
        assertEquals(1, entity.getOccurrences());
        assertEquals(date, entity.getMentionTime());

        entity = entities.get(0);
        assertEquals(ent2, entity.getEntityValue());
        assertEquals(1, entity.getOccurrences());
        assertEquals(date, entity.getMentionTime());
    }

    @After
    public void tearDown() throws Exception {
        underTest.cleanup();
    }
}
