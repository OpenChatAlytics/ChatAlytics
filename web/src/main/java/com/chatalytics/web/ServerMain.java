package com.chatalytics.web;

import com.chatalytics.compute.util.YamlUtils;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.web.resources.EventsResource;
import com.chatalytics.web.resources.TopEmojisResource;
import com.chatalytics.web.resources.TrendingTopicsResource;
import com.google.common.collect.Sets;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;

import java.util.Set;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;
import javax.ws.rs.core.Application;

/**
 * Entry point to the web resources
 *
 * @author giannis
 *
 */
public class ServerMain extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);
    public static final int PORT = 8080;

    private final ChatAlyticsConfig config;
    private final RealtimeComputeClient realtimeComputeClient;

    public ServerMain(ChatAlyticsConfig config, RealtimeComputeClient realtimeComputeClient) {
        this.config = config;
        this.realtimeComputeClient = realtimeComputeClient;

        // Sets up classpath scanning for Swagger + JAXRS
        // Resources available at localhost:8080/swagger.json
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setBasePath("http://localhost:8080/api");
        beanConfig.setResourcePackage("com.chatalytics.web.resources");
        beanConfig.setScan(true);
    }

    public static void main(String[] args) throws Exception {
        ChatAlyticsConfig config = YamlUtils.readYamlFromResource("chatalytics.yaml",
                                                                  ChatAlyticsConfig.class);

        EventsResource eventResource = new EventsResource();
        RealtimeComputeClient computeClient = new RealtimeComputeClient(config, eventResource);
        ServerMain serverMain = new ServerMain(config, computeClient);
        serverMain.startComputeClient();

        // Start the server
        Server server = new Server(PORT);
        ServletContainer servletContainer = new ServletContainer(serverMain);
        ServletHolder servletHolder = new ServletHolder("/*", servletContainer);
        ServletContextHandler context = new ServletContextHandler();
        context.addServlet(servletHolder, "/*");
        server.setHandler(context);
        setWebSocketEndpoints(context, eventResource);

        server.start();
        server.join();
        computeClient.stopAsync().awaitTerminated();
    }

    @Override
    public Set<Object> getSingletons() {
        return Sets.newHashSet(new TrendingTopicsResource(config),
                               new TopEmojisResource(config),
                               new ApiListingResource());
    }

    protected void startComputeClient() {
        if (!realtimeComputeClient.isRunning()) {
            try {
                realtimeComputeClient.startAsync().awaitRunning();
            } catch (Exception e) {
                LOG.error("Unable to connect to RT compute stream. No data will be streamed");
            }
        }
    }

    /**
     *
     * @param context the context to add the web socket endpoints to
     * @param rtEventResource The instance of the websocket endpoint to return
     * @throws DeploymentException
     */
    private static void setWebSocketEndpoints(ServletContextHandler context,
                                              EventsResource rtEventResource)
            throws DeploymentException, ServletException {

        ServerContainer wsContainer = WebSocketServerContainerInitializer.configureContext(context);

        ServerEndpointConfig serverConfig =
                ServerEndpointConfig.Builder
                                    .create(EventsResource.class, EventsResource.RT_EVENT_ENDPOINT)
                                    .configurator(new Configurator() {
                                        @Override
                                        public <T> T getEndpointInstance(Class<T> endpointClass)
                                                throws InstantiationException {
                                            return endpointClass.cast(rtEventResource);
                                        }
                                    }).build();

        wsContainer.addEndpoint(serverConfig);
    }

}
