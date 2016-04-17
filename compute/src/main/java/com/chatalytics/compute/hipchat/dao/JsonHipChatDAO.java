package com.chatalytics.compute.hipchat.dao;

import com.chatalytics.compute.chat.dao.AbstractJSONChatApiDAO;
import com.chatalytics.compute.chat.dao.IChatApiDAO;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.json.JsonObjectMapperFactory;
import com.chatalytics.core.model.Message;
import com.chatalytics.core.model.Room;
import com.chatalytics.core.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON implementation of the {@link IChatApiDAO} for HipChat
 *
 * @author giannis
 *
 */
public class JsonHipChatDAO extends AbstractJSONChatApiDAO {

    private static final String AUTH_TOKEN_PARAM = "auth_token";
    private static final Logger LOG = LoggerFactory.getLogger(JsonHipChatDAO.class);

    private final WebResource resource;
    private final ChatAlyticsConfig config;
    private final ObjectMapper objMapper;

    public final DateTimeZone dtz;
    public final DateTimeFormatter apiDateFormat;

    public JsonHipChatDAO(ChatAlyticsConfig config, Client client) {
        super(config.hipchatConfig.authTokens, AUTH_TOKEN_PARAM);
        this.resource = client.resource(config.hipchatConfig.baseHipChatURL);
        this.config = config;
        this.dtz = DateTimeZone.forID(config.timeZone);
        this.apiDateFormat = DateTimeFormat.forPattern(config.apiDateFormat).withZone(dtz);
        this.objMapper = JsonObjectMapperFactory.createObjectMapper(config.inputType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Room> getRooms() {
        WebResource roomResource = resource.path("rooms/list");
        String jsonStr = getJsonResultWithRetries(roomResource, config.apiRetries);
        Collection<Room> roomCol = deserializeJsonStr(jsonStr, "rooms", Room.class, objMapper);
        Map<String, Room> result = Maps.newHashMapWithExpectedSize(roomCol.size());
        for (Room room : roomCol) {
            result.put(room.getRoomId(), room);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, User> getUsers() {
        WebResource userResource = resource.path("users/list");
        String jsonStr = getJsonResultWithRetries(userResource, config.apiRetries);
        Collection<User> userCol = deserializeJsonStr(jsonStr, "users", User.class, objMapper);
        Map<String, User> result = Maps.newHashMapWithExpectedSize(userCol.size());
        for (User user : userCol) {
            result.put(user.getUserId(), user);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, User> getUsersForRoom(Room room) {
        throw new UnsupportedOperationException("get users for room is not supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessages(DateTime start, DateTime end, Room room) {
        DateTime curDate = start;
        List<Message> messages = Lists.newArrayList();
        WebResource roomsResource = resource.path("rooms/history");
        roomsResource = roomsResource.queryParam("room_id", room.getRoomId())
                                     .queryParam("timezone", config.timeZone);
        Interval messageInterval = new Interval(start, end);
        while (curDate.isBefore(end) || curDate.equals(end)) {
            roomsResource = roomsResource.queryParam("date", curDate.toString(apiDateFormat));
            String jsonStr = getJsonResultWithRetries(roomsResource, config.apiRetries);
            Collection<Message> messageCol = deserializeJsonStr(jsonStr, "messages", Message.class,
                                                                objMapper);
            for (Message message : messageCol) {
                if (messageInterval.contains(message.getDate())) {
                    messages.add(message);
                }
            }
            curDate = curDate.plusDays(1);
        }
        return messages;
    }

    /**
     * Helper method for deserializing a chat JSON response to a collection of objects.
     *
     * @param jsonStr
     *            The chat JSON response.
     * @param mapElement
     *            Chat JSON responses are actually maps with a single element. This argument is
     *            the value of the element to pull out from the map.
     * @param colClassElements
     *            The types of objects that the collection object will contain.
     * @param objMapper
     *            The JSON object mapper used to deserialize the JSON string.
     * @return A collection of elements of type <code>colClassElements</code>.
     */
    private <T> Collection<T> deserializeJsonStr(String jsonStr, String mapElement,
                                                 Class<T> colClassElements,
                                                 ObjectMapper objMapper) {
        Map<String, Collection<T>> re;
        try {
            TypeFactory typeFactory = objMapper.getTypeFactory();
            CollectionType type = typeFactory.constructCollectionType(List.class, colClassElements);
            MapType thetype = typeFactory.constructMapType(HashMap.class,
                                                           typeFactory.constructType(String.class),
                                                           type);
            re = objMapper.readValue(jsonStr, thetype);
        } catch (IOException e) {
            LOG.error("Got exception when trying to deserialize list of {}", colClassElements, e);
            return Lists.newArrayListWithExpectedSize(0);
        }
        return re.get(mapElement);
    }
}
