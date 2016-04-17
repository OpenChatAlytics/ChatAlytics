package com.chatalytics.core.realtime.json;

import com.chatalytics.core.model.ChatAlyticsEvent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import org.joda.time.DateTime;

import java.io.IOException;
import java.io.Serializable;

/**
 * Deserializer specific to {@link ChatAlyticsEvent}s. This exists because it understands how to
 * deserialize arbitrary event types based on a helper class type field
 *
 * @author giannis
 *
 */
public class ChatAlyticsEventDeserializer extends JsonDeserializer<ChatAlyticsEvent> {

    @Override
    public ChatAlyticsEvent deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);

        JsonParser eventTimeParser = node.get("event_time").traverse();
        DateTime eventTime = oc.readValue(eventTimeParser, DateTime.class);

        String type = node.get("type").asText();

        Class<?> clazz;
        String classStr = node.get("clazz").asText();
        try {
            clazz = Class.forName(classStr);
        } catch (ClassNotFoundException e) {
            throw new IOException("Can't load class for " + classStr, e);
        }

        JsonParser eventParser = node.get("event").traverse();
        Serializable event = (Serializable) oc.readValue(eventParser, clazz);

        return new ChatAlyticsEvent(eventTime, type, event);
    }

}
