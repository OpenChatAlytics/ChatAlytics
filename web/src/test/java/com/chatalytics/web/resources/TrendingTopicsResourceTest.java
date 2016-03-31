package com.chatalytics.web.resources;

import com.chatalytics.compute.db.dao.IEntityDAO;
import com.chatalytics.compute.db.dao.EntityDAOImpl;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.model.ChatEntity;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link TrendingTopicsResource}.
 *
 * @author giannis
 *
 */
public class TrendingTopicsResourceTest {

    private IEntityDAO entityDao;
    private TrendingTopicsResource underTest;
    private DateTimeZone dtZone;
    private DateTime mentionTime;

    @Before
    public void setUp() throws Exception {
        ChatAlyticsConfig config = new ChatAlyticsConfig();
        config.persistenceUnitName = "chatalytics-web-test";
        config.timeZone = "America/New_York";
        dtZone = DateTimeZone.forID(config.timeZone);

        entityDao = new EntityDAOImpl(config);
        entityDao.startAsync().awaitRunning();

        mentionTime = DateTime.now().withZone(DateTimeZone.UTC);
        List<ChatEntity> entities = Lists.newArrayListWithCapacity(10);
        entities.add(new ChatEntity("e1", 5, mentionTime.minusHours(1), "u1", "r1"));
        entities.add(new ChatEntity("e1", 4, mentionTime.minusHours(1), "u2", "r1"));
        entities.add(new ChatEntity("e1", 1, mentionTime.minusHours(2), "u3", "r2"));
        entities.add(new ChatEntity("e1", 1, mentionTime.minusHours(3), "u4", "r3"));
        entities.add(new ChatEntity("e2", 1, mentionTime.minusHours(1), "u1", "r1"));
        entities.add(new ChatEntity("e2", 6, mentionTime.minusHours(1), "u2", "r1"));
        entities.add(new ChatEntity("e2", 7, mentionTime.minusHours(2), "u3", "r2"));
        entities.add(new ChatEntity("e3", 3, mentionTime.minusHours(1), "u2", "r2"));
        entities.add(new ChatEntity("e3", 3, mentionTime.minusHours(2), "u3", "r1"));
        entities.add(new ChatEntity("e4", 3, mentionTime.minusHours(1), "u1", "r4"));
        storeTestEntities(entities);
        underTest = new TrendingTopicsResource(config);
    }

    private void storeTestEntities(List<ChatEntity> entities) {
        for (ChatEntity entity : entities) {
            entityDao.persistEntity(entity);
        }
    }

    @After
    public void tearDown() throws Exception {
        entityDao.stopAsync().awaitTerminated();
    }

    /**
     * Tests to see if the correct trending topics are returned
     */
    @Test
    public void testGetTrendingTopics() throws Exception {
        DateTimeFormatter dtf = TrendingTopicsResource.PARAMETER_WITH_DAY_DTF.withZone(dtZone);
        String startTimeStr = dtf.print(mentionTime.minusDays(1));
        String endTimeStr = dtf.print(mentionTime.plusDays(1));
        Response response = underTest.getTrendingTopics(startTimeStr, endTimeStr, "u1", "r1");
        assertEquals("{\"e1\":5,\"e2\":1}", response.getEntity());

        response = underTest.getTrendingTopics(startTimeStr, endTimeStr, "u1", null);
        assertEquals("{\"e1\":5,\"e4\":3,\"e2\":1}", response.getEntity());

        response = underTest.getTrendingTopics(startTimeStr, endTimeStr, null, null);
        assertEquals("{\"e2\":14,\"e1\":11,\"e3\":6,\"e4\":3}", response.getEntity());
    }

    /**
     * Tests to see if the correct date with the right timezone is returned when parsing parameters
     */
    @Test
    public void testGetDateTimeFromParameter() {
        DateTime dateTime = underTest.getDateTimeFromParameter("2015-01-01");
        DateTime expectedDateTime =
            new DateTime(2015, 1, 1, 0, 0).withZone(dtZone).toDateTime(DateTimeZone.UTC);
        assertEquals(expectedDateTime, dateTime);
    }

}
