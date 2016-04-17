package com.chatalytics.core.realtime;

import com.chatalytics.core.json.JsonObjectMapperFactory;
import com.chatalytics.core.model.ChatAlyticsEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * Encoder for passing {@link ChatAlyticsEvent}s through sockets
 *
 * @author giannis
 *
 */
public class ChatAlyticsEventEncoder implements Encoder.Text<ChatAlyticsEvent> {

    private final ObjectMapper objectMapper;

    public ChatAlyticsEventEncoder() {
        objectMapper = JsonObjectMapperFactory.createObjectMapper();
    }

    @Override
    public void init(EndpointConfig config) { }

    @Override
    public String encode(ChatAlyticsEvent object) throws EncodeException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new EncodeException(object, "Can't encode object. Reason: " + e.getMessage());
        }
    }

    @Override
    public void destroy() {
        // no op
    }

}
