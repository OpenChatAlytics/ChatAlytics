package com.chatalytics.compute.storm.bolt;

import com.chatalytics.compute.config.ConfigurationConstants;
import com.chatalytics.compute.util.YamlUtils;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.model.ChatAlyticsEvent;
import com.chatalytics.core.realtime.ChatAlyticsEventEncoder;
import com.chatalytics.core.realtime.ConnectionType;
import com.chatalytics.core.realtime.ConnectionTypeEncoderDecoder;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import static com.chatalytics.compute.realtime.RealtimeResource.RT_COMPUTE_ENDPOINT;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;

/**
 * Realtime bolt that can subscribe to any ChatAlytics {@link Serializable} object and publish it
 * out to the socket clients.
 *
 * @author giannis
 */
@ClientEndpoint(encoders = { ChatAlyticsEventEncoder.class, ConnectionTypeEncoderDecoder.class })
public class RealtimeBolt extends BaseRichBolt {

    private static final long serialVersionUID = -214311696491358951L;
    private static final Logger LOG = LoggerFactory.getLogger(RealtimeBolt.class);
    public static final String BOLT_ID = "RT_SOCKET_BOLT_ID";
    private Session session;

    @Override
    public void prepare(@SuppressWarnings("rawtypes") Map conf, TopologyContext context,
                        OutputCollector collector) {
        String configStr = (String) conf.get(ConfigurationConstants.CHATALYTICS_CONFIG.txt);
        ChatAlyticsConfig config = YamlUtils.readYamlFromString(configStr, ChatAlyticsConfig.class);

        WebSocketContainer webSocketContainer = getWebSocketContainer();
        this.session = openRealtimeConnection(webSocketContainer, config);
    }

    @Override
    public void execute(Tuple input) {
        for (Object obj : input.getValues()) {
            Serializable serObj;
            if (obj instanceof Serializable) {
                serObj = (Serializable) obj;
            } else {
                LOG.warn("Received a non-serializable object. Skipping...");
                continue;
            }

            String type = UPPER_CAMEL.to(LOWER_UNDERSCORE, serObj.getClass().getSimpleName());
            ChatAlyticsEvent event = new ChatAlyticsEvent(DateTime.now(DateTimeZone.UTC),
                                                          type,
                                                          serObj);

            publishEvent(event);
        }
    }

    private void publishEvent(ChatAlyticsEvent event) {
        try {
            session.getBasicRemote().sendObject(event);
        } catch (IOException | EncodeException e) {
            LOG.error("Can't publish event to realtime compute server. {}", event, e);
        }
    }

    /**
     * Opens a connection to the compute socket. This method will return an optional session. If the
     * session is absent then this resource will reject user connections
     *
     * @param webSocketContainer
     *            The container
     * @param config
     *            ChatAlytics config
     * @return An optional session
     */
    private Session openRealtimeConnection(WebSocketContainer webSocketContainer,
                                                     ChatAlyticsConfig config) {
        URI rtURI = URI.create(String.format("ws://localhost:%d%s/%s",
                                             config.rtComputePort,
                                             RT_COMPUTE_ENDPOINT,
                                             ConnectionType.PUBLISHER));
        try {
            LOG.info("Connecting to {}", rtURI);
            return webSocketContainer.connectToServer(this, rtURI);
        } catch (DeploymentException | IOException e) {
            throw new RuntimeException("Unable to connect to RT compute server. Is it up?");
        }
    }

    private WebSocketContainer getWebSocketContainer() {
        return ContainerProvider.getWebSocketContainer();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer fields) {
        // no output
    }
}
