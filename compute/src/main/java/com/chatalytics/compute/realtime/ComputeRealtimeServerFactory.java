package com.chatalytics.compute.realtime;

import com.chatalytics.core.config.ChatAlyticsConfig;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

/**
 * Factory for building the realtime compute server
 *
 * @author giannis
 */
public class ComputeRealtimeServerFactory {

    /**
     * Creates a new {@link ComputeRealtimeServer}
     *
     * @param config
     *            The chatalytics config
     * @return A newly created {@link ComputeRealtimeServer}
     */
    public static ComputeRealtimeServer createComputeRealtimeServer(ChatAlyticsConfig config) {
        Server server = new Server(config.rtComputePort);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServerContainer wscontainer;
        try {
            wscontainer = WebSocketServerContainerInitializer.configureContext(context);
            wscontainer.addEndpoint(RealtimeResource.class);
        } catch (ServletException | DeploymentException e) {
            throw new RuntimeException("Can't instantiate websocket. Reason: " + e.getMessage());
        }

        return new ComputeRealtimeServer(server);
    }

}
