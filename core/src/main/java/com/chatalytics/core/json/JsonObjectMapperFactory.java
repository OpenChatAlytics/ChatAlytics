package com.chatalytics.core.json;

import com.chatalytics.core.InputSourceType;
import com.chatalytics.core.model.ChatAlyticsEvent;
import com.chatalytics.core.model.hipchat.json.HipChatJsonModule;
import com.chatalytics.core.model.slack.json.SlackJsonModule;
import com.chatalytics.core.realtime.json.ChatAlyticsEventDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Builds a JSON {@link ObjectMapper} for deserializing and serializing objects.
 *
 * @author giannis
 *
 */
public class JsonObjectMapperFactory {

    private JsonObjectMapperFactory() {}

    /**
     * Creates an {@link ObjectMapper} for a specific input source. Use this if you want to decode
     * messages from either Slack or HipChat
     *
     * @param inputSource
     *            The input source type
     * @return An {@link ObjectMapper} that can deserialize events from a chat input source
     */
    public static ObjectMapper createObjectMapper(InputSourceType inputSource) {
        ObjectMapper objectMapper = createObjectMapper();

        if (inputSource == InputSourceType.SLACK || inputSource == InputSourceType.SLACK_BACKFILL) {
            objectMapper.registerModule(new SlackJsonModule());
        } else if (inputSource == InputSourceType.HIPCHAT) {
            objectMapper.registerModule(new HipChatJsonModule());
        }

        return objectMapper;
    }

    /**
     * Creates an {@link ObjectMapper} for the ChatAlytics specific objects
     *
     * @return An {@link ObjectMapper} that can deserialize ChatAlytics specific objects
     */
    public static ObjectMapper createObjectMapper() {

        SimpleModule commonModule = new SimpleModule();
        commonModule.addDeserializer(ChatAlyticsEvent.class, new ChatAlyticsEventDeserializer());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.registerModule(new JodaModule());
        objectMapper.registerModule(commonModule);

        return objectMapper;
    }

}
