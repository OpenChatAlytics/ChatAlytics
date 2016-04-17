package com.chatalytics.compute.storm.spout;

import com.chatalytics.core.InputSourceType;
import com.chatalytics.core.json.JsonObjectMapperFactory;
import com.chatalytics.core.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

/**
 * This decoder can be attached to a slack RTM handler to decode Message types
 *
 * @author giannis
 */
public class WebSocketMessageDecoder implements Decoder.Text<Message> {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketMessageDecoder.class);
    private static final String MESSAGE_TYPE_SLACK = "message";

    private ObjectMapper objMapper;

    @Override
    public void init(EndpointConfig config) {
        objMapper = JsonObjectMapperFactory.createObjectMapper(InputSourceType.SLACK);
    }

    @Override
    public Message decode(String event) throws DecodeException {
        try {
            return objMapper.readValue(event, Message.class);
        } catch (IOException e) {
            throw new DecodeException("Could not decode event", e.getMessage());
        }
    }

    /**
     * @return True if the type of event is a message and false otherwise.
     */
    @Override
    public boolean willDecode(String eventStr) {
        try {
            String type = objMapper.readTree(eventStr).get("type").asText();
            if (MESSAGE_TYPE_SLACK.equals(type)) {
                return true;
            }
        } catch (IOException e) {
            LOG.error("Could not determine type for {}. Will not process event...", eventStr);
            return false;
        }
        return false;
    }

    @Override
    public void destroy() {
        // no-op
    }

}
