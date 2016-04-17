package com.chatalytics.compute.slack.dao;

import com.chatalytics.compute.util.YamlUtils;
import com.chatalytics.core.InputSourceType;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.model.Message;
import com.chatalytics.core.model.Room;
import com.chatalytics.core.model.User;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests {@link JsonSlackDAO} by spying it and making it return a JSON value.
 *
 * @author giannis
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSlackDAOTest {

    private JsonSlackDAO underTest;
    private WebResource mockResource;
    private ChatAlyticsConfig config;

    @Before
    public void setUp() throws Exception {
        this.config = YamlUtils.readYamlFromResource("chatalytics.yaml", ChatAlyticsConfig.class);
        config.inputType = InputSourceType.SLACK;
        Client mockClient = mock(Client.class);
        mockResource = mock(WebResource.class);
        when(mockClient.resource(config.slackConfig.baseSlackURL)).thenReturn(mockResource);
        underTest = spy(new JsonSlackDAO(config, mockClient));
    }

    /**
     * Makes sure rooms can be returned
     */
    @Test
    public void testGetRooms() throws Exception {

        // channels.list
        WebResource mockChanResource = mock(WebResource.class);
        when(mockResource.path("channels.list")).thenReturn(mockChanResource);
        URI channelListURI = Resources.getResource("slack_api_responses/channels.list.txt").toURI();
        Path channelsPath = Paths.get(channelListURI);
        String channelsResponseStr = new String(Files.readAllBytes(channelsPath));
        doReturn(channelsResponseStr).when(underTest).getJsonResultWithRetries(mockChanResource,
                                                                               config.apiRetries);

        Map<String, Room> rooms = underTest.getRooms();
        assertEquals(2, rooms.size());
        for (Room room : rooms.values()) {
            assertNotNull(room);
        }
    }

    /**
     * Makes sure users can be returned
     */
    @Test
    public void testGetUsers() throws Exception {

        // users.list
        WebResource mockUserResource = mock(WebResource.class);
        when(mockResource.path("users.list")).thenReturn(mockUserResource);
        URI userListURI = Resources.getResource("slack_api_responses/users.list.txt").toURI();
        Path usersPath = Paths.get(userListURI);
        String usersResponseStr = new String(Files.readAllBytes(usersPath));
        doReturn(usersResponseStr).when(underTest).getJsonResultWithRetries(mockUserResource,
                                                                            config.apiRetries);

        Map<String, User> users = underTest.getUsers();
        assertEquals(2, users.size());
        for (User user : users.values()) {
            assertNotNull(user);
        }
    }

    /**
     * Makes sure users for a given room are properly returned
     */
    @Test
    public void testGetUsersForRoom() throws Exception {

        // channels.info
        WebResource mockChanInfoResrc = mock(WebResource.class);
        when(mockResource.path("channels.info")).thenReturn(mockChanInfoResrc);
        when(mockChanInfoResrc.queryParam(anyString(), anyString())).thenReturn(mockChanInfoResrc);
        URI chanInfoURI = Resources.getResource("slack_api_responses/channels.info.txt").toURI();
        Path channelsInfoPath = Paths.get(chanInfoURI);
        String chanInfoResponseStr = new String(Files.readAllBytes(channelsInfoPath));
        doReturn(chanInfoResponseStr).when(underTest).getJsonResultWithRetries(mockChanInfoResrc,
                                                                               config.apiRetries);

        // users.info
        WebResource mockUserInfoResource = mock(WebResource.class);
        when(mockResource.path("users.info")).thenReturn(mockUserInfoResource);
        // user 1
        WebResource user1InfoResource = mock(WebResource.class);
        when(mockUserInfoResource.queryParam("user", "U023BECGF")).thenReturn(user1InfoResource);
        URI userInfoURI = Resources.getResource("slack_api_responses/users.info.1.txt").toURI();
        Path usersInfoPath = Paths.get(userInfoURI);
        String userInfoResponseStr = new String(Files.readAllBytes(usersInfoPath));
        doReturn(userInfoResponseStr).when(underTest).getJsonResultWithRetries(user1InfoResource,
                                                                               config.apiRetries);

        // user 2
        WebResource user2InfoResource = mock(WebResource.class);
        when(mockUserInfoResource.queryParam("user", "U023TY454")).thenReturn(user2InfoResource);
        userInfoURI = Resources.getResource("slack_api_responses/users.info.2.txt").toURI();
        usersInfoPath = Paths.get(userInfoURI);
        userInfoResponseStr = new String(Files.readAllBytes(usersInfoPath));
        doReturn(userInfoResponseStr).when(underTest).getJsonResultWithRetries(user2InfoResource,
                                                                               config.apiRetries);

        Room mockRoom = mock(Room.class);
        when(mockRoom.getRoomId()).thenReturn("C0SDFG423");
        Map<String, User> usersForRoom = underTest.getUsersForRoom(mockRoom);
        assertEquals(2, usersForRoom.size());
        for (User user : usersForRoom.values()) {
            assertNotNull(user);
        }
    }

    /**
     * Makes sure messages for a given room and time interval are properly returned
     */
    @Test
    public void testGetMessages() throws Exception {

        // channels.history
        WebResource mockHistoryResrc = mock(WebResource.class);
        when(mockResource.path("channels.history")).thenReturn(mockHistoryResrc);
        when(mockHistoryResrc.queryParam(anyString(), anyString())).thenReturn(mockHistoryResrc);
        URI historyURI = Resources.getResource("slack_api_responses/channels.history.txt").toURI();
        Path historyPath = Paths.get(historyURI);
        String historyResponseStr = new String(Files.readAllBytes(historyPath));
        doReturn(historyResponseStr).when(underTest).getJsonResultWithRetries(mockHistoryResrc,
                                                                              config.apiRetries);

        Room mockRoom = mock(Room.class);
        when(mockRoom.getRoomId()).thenReturn("C0SDFG423");
        DateTime now = DateTime.now();

        List<Message> messages = underTest.getMessages(now.minusDays(1), now, mockRoom);
        assertEquals(7, messages.size());
        for (Message message : messages) {
            assertNotNull(message);
        }
    }

    /**
     * Makes sure that the web socket URL can be properly read
     */
    @Test
    public void testGetRealtimeWebSocketURI() throws Exception {

        // RTM start
        WebResource mockRtmResrc = mock(WebResource.class);
        when(mockResource.path("rtm.start")).thenReturn(mockRtmResrc);
        String rtmResponseStr =
            "{\"ok\": true, \"url\":\"wss:\\/\\/ms9.slack-msgs.com\\/websocket\\/7I5yBpcvk\"}";
        doReturn(rtmResponseStr).when(underTest).getJsonResultWithRetries(mockRtmResrc,
                                                                          config.apiRetries);

        assertEquals(URI.create("wss://ms9.slack-msgs.com/websocket/7I5yBpcvk"),
                     underTest.getRealtimeWebSocketURI());
    }

    /**
     * Makes sure that an exception is propagated when a malformed JSON is returned from the API
     */
    @Test(expected = IOException.class)
    public void testGetRealtimeWebSocketURI_withoutURLInResponse() throws Exception {
        // RTM start
        WebResource mockRtmResrc = mock(WebResource.class);
        when(mockResource.path("rtm.start")).thenReturn(mockRtmResrc);
        String rtmResponseStr = "{malformedJSON";
        doReturn(rtmResponseStr).when(underTest).getJsonResultWithRetries(mockRtmResrc,
                                                                          config.apiRetries);
        try {
            underTest.getRealtimeWebSocketURI();
        } catch (Exception e) {
            throw (Exception) e.getCause();
        }

    }

}
