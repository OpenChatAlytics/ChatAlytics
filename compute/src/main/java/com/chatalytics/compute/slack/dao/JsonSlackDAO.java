package com.chatalytics.compute.slack.dao;

import com.chatalytics.compute.chat.dao.AbstractJSONChatApiDAO;
import com.chatalytics.compute.chat.dao.IChatApiDAO;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.json.JsonObjectMapperFactory;
import com.chatalytics.core.model.Message;
import com.chatalytics.core.model.Room;
import com.chatalytics.core.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JSON implementation of the {@link IChatApiDAO} for Slack
 *
 * @author giannis
 *
 */
public class JsonSlackDAO extends AbstractJSONChatApiDAO {

    private static final String AUTH_TOKEN_PARAM = "token";
    private static final Logger LOG = LoggerFactory.getLogger(JsonSlackDAO.class);

    private final WebResource resource;
    private final ChatAlyticsConfig config;
    private final ObjectMapper objMapper;

    public JsonSlackDAO(ChatAlyticsConfig config, Client client) {
        super(config.slackConfig.authTokens, AUTH_TOKEN_PARAM);
        this.resource = client.resource(config.slackConfig.baseSlackURL);
        this.config = config;
        this.objMapper = JsonObjectMapperFactory.createObjectMapper(config.inputType);
    }

    @Override
    public Map<String, Room> getRooms() {
        WebResource roomResource = resource.path("channels.list");
        String jsonStr = getJsonResultWithRetries(roomResource, config.apiRetries);
        Collection<Room> roomCol = deserializeJsonStr(jsonStr, "channels", Room.class, objMapper);
        Map<String, Room> result = Maps.newHashMapWithExpectedSize(roomCol.size());
        for (Room room : roomCol) {
            result.put(room.getRoomId(), room);
        }
        return result;
    }

    @Override
    public Map<String, User> getUsers() {
        WebResource userResource = resource.path("users.list");
        String jsonStr = getJsonResultWithRetries(userResource, config.apiRetries);
        Collection<User> userCol = deserializeJsonStr(jsonStr, "members", User.class, objMapper);
        Map<String, User> result = Maps.newHashMapWithExpectedSize(userCol.size());
        for (User user : userCol) {
            result.put(user.getUserId(), user);
        }
        return result;
    }

    @Override
    public Map<String, User> getUsersForRoom(Room room) {
        WebResource roomResource = resource.path("channels.info");
        roomResource = roomResource.queryParam("channel", room.getRoomId());
        String jsonStr = getJsonResultWithRetries(roomResource, config.apiRetries);
        Collection<String> userIdCol = deserializeJsonStr(jsonStr,
                                                          Lists.newArrayList("channel", "members"),
                                                          String.class,
                                                          objMapper);
        // get info for user IDs
        Map<String, User> result = Maps.newHashMapWithExpectedSize(userIdCol.size());
        for (String userId : userIdCol) {
            WebResource userResource = resource.path("users.info");
            userResource = userResource.queryParam("user", userId);
            jsonStr = getJsonResultWithRetries(userResource, config.apiRetries);
            try {
                JsonNode jsonNode = objMapper.readTree(jsonStr);
                jsonNode = jsonNode.get("user");
                User user = objMapper.readValue(jsonNode.toString(), User.class);
                result.put(user.getUserId(), user);
            } catch (IOException e) {
                throw new RuntimeException("Can't deserialize user with ID:" + userId, e);
            }
        }
        return result;
    }

    /**
     * @return A URI for initiating the realtime web socket connection
     */
    public URI getRealtimeWebSocketURI() {
        WebResource rtmResource = resource.path("rtm.start");
        String jsonStr = getJsonResultWithRetries(rtmResource, config.apiRetries);
        try {
            String webSocketUrl = objMapper.readTree(jsonStr).get("url").asText();
            return URI.create(webSocketUrl);
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse realtime resource response", e);

        }
    }

    @Override
    public List<Message> getMessages(DateTime start, DateTime end, Room room) {
        long startMillis = start.getMillis();
        String startMillisStr = String.format("%s.%s",
                                              String.valueOf(startMillis / 1000),
                                              startMillis % 1000);

        // get smallest time unit for slack API and subtract one because latest is inclusive in the
        // Slack API
        long endNanos = end.getMillis() * 1000 - 1;
        String endMillisStr = String.format("%s.%s",
                                            String.valueOf(endNanos / 1000 / 1000),
                                            endNanos % (1000 * 1000));

        WebResource roomResource = resource.path("channels.history");
        roomResource = roomResource.queryParam("channel", room.getRoomId())
                                   .queryParam("latest", endMillisStr)
                                   .queryParam("oldest", startMillisStr)
                                   .queryParam("inclusive", "1")
                                   .queryParam("count", "1000");

        String jsonStr = getJsonResultWithRetries(roomResource, config.apiRetries);
        Collection<Message> messagesCol = deserializeJsonStr(jsonStr,
                                                             "messages",
                                                             Message.class,
                                                             objMapper);
        return Lists.newArrayList(messagesCol);
    }

    /**
     * Helper method for deserializing lists of elements of type <code>T</code> from a response JSON
     * string
     *
     * @param jsonStr
     *            The API returned JSON string
     * @param listElement
     *            The element in the original response JSON string that contains the list of items
     *            of type <code>T</code> to deserialize
     * @param clazz
     *            The class to deserialize
     * @param objMapper
     *            The JSON object mapper used to deserialize the JSON string.
     * @return A collection of elements of type <code>clazz</code>.
     */
    private <T> Collection<T> deserializeJsonStr(String jsonStr, String listElement,
                                                 Class<T> clazz,
                                                 ObjectMapper objMapper) {
        return deserializeJsonStr(jsonStr, Lists.newArrayList(listElement), clazz, objMapper);
    }

    /**
     * Helper method for deserializing lists of elements of type <code>T</code> from a response JSON
     * string
     *
     * @param jsonStr
     *            The API returned JSON string
     * @param listElements
     *            A list of ordered elements in the original response JSON to traverse down to to
     *            get to the element that contains the list of items of type <code>T</code> to
     *            deserialize
     *            <p/>
     *            For example given the following JSON string
     *
     * <pre>
     *            {
     *                "someElement": {
     *                    "someInnerElement": [
     *                        {...},
     *                        {...}
     *                    ],
     *                    "otherElement": "value"
     *                }
     *            }
     * </pre>
     *
     *            If the collection of <code>T</code>s we were interested in were inside
     *            <code>someInnerEllement</code> then the list would equal:
     *            <code>[someElement, someInnterElement]</code>
     * @param clazz
     *            The class to deserialize The JSON object mapper used to deserialize the JSON
     *            string.
     * @return A collection of elements of type <code>clazz</code>.
     */
    private <T> Collection<T> deserializeJsonStr(String jsonStr, List<String> listElements,
                                                 Class<T> clazz,
                                                 ObjectMapper objMapper) {
        TypeFactory typeFactory = objMapper.getTypeFactory();
        CollectionType type = typeFactory.constructCollectionType(List.class, clazz);
        try {
            JsonNode jsonNode = objMapper.readTree(jsonStr);
            // traverse down the order of elements until the desired one is reached
            for (String jsonEl : listElements) {
                jsonNode = jsonNode.get(jsonEl);
                if (jsonNode == null) {
                    return Lists.newArrayListWithCapacity(0);
                }
            }
            return objMapper.readValue(jsonNode.toString(), type);
        } catch (IOException e) {
            LOG.error("Got exception when trying to deserialize list of {}", clazz, e);
            return Lists.newArrayListWithExpectedSize(0);
        }
    }
}
