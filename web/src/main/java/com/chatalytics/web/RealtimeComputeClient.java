package com.chatalytics.web;

import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.realtime.ConnectionType;
import com.chatalytics.web.resources.EventsResource;
import com.google.common.util.concurrent.AbstractIdleService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Client that knows how to connect to the compute realtime web server. This client has a
 * {@link #stopAsync()} method which should be called when done
 *
 * @author giannis
 */
public class RealtimeComputeClient extends AbstractIdleService {

    private static Logger LOG = LoggerFactory.getLogger(RealtimeComputeClient.class);

    private final ChatAlyticsConfig config;
    private final EventsResource eventResource;
    private Session session;

    public RealtimeComputeClient(ChatAlyticsConfig config, EventsResource eventResource) {
        this.config = config;
        this.eventResource = eventResource;
    }

    private WebSocketContainer getWebSocketContainer() {
        return ContainerProvider.getWebSocketContainer();
    }

    @Override
    protected void startUp() throws Exception {
        WebSocketContainer webSocketContainer = getWebSocketContainer();
        this.session = openRealtimeConnection(webSocketContainer, config);
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
            ChatAlyticsConfig config) throws DeploymentException, IOException {
        URI rtURI = URI.create(String.format("ws://localhost:%d%s/%s",
                                             config.rtComputePort,
                                             config.rtComputePath,
                                             ConnectionType.SUBSCRIBER));
        Session session = webSocketContainer.connectToServer(eventResource, rtURI);
        LOG.info("Connected to realtime compute server");
        return session;
    }

    /**
     * Closes the compute realtime socket session
     */
    @Override
    protected void shutDown() throws Exception {
        if (session != null) {
            session.close();
        }
    }

}
