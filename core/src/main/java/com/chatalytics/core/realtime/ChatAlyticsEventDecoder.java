package com.chatalytics.core.realtime;

import com.chatalytics.core.json.JsonObjectMapperFactory;
import com.chatalytics.core.model.ChatAlyticsEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import java.io.IOException;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 * Socket decoder that knows how to decode {@link ChatAlyticsEvent}s through sockets
 *
 * @author giannis
 *
 */
public class ChatAlyticsEventDecoder implements Decoder.Text<ChatAlyticsEvent> {

    private final ObjectMapper objectMapper;

    public ChatAlyticsEventDecoder() {
        objectMapper = JsonObjectMapperFactory.createObjectMapper();
    }

    @Override
    public void init(EndpointConfig config) { }

    /**
     * Decodes the string to a {@link ChatAlyticsEvent}
     *
     * @param str
     *            The string to deserialize
     * @return A deserialized {@link ChatAlyticsEvent}
     */
    @Override
    public ChatAlyticsEvent decode(String str) throws DecodeException {
        try {
            return objectMapper.readValue(str, ChatAlyticsEvent.class);
        } catch (IOException e) {
            throw new DecodeException("Could not decode event", e.getMessage());
        }
    }

    /**
     * Decides whether it can deode a string message
     *
     * @param str
     *            The string to decide whether it can be decoded
     * @return Returns true if <code>str</code> is not null or empty
     */
    @Override
    public boolean willDecode(String str) {
        return !Strings.isNullOrEmpty(str);
    }

    @Override
    public void destroy() {
        // no-op
    }
}
