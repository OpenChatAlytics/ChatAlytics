package com.chatalytics.web.resources;

import com.chatalytics.core.model.ChatAlyticsEvent;
import com.chatalytics.core.realtime.ChatAlyticsEventDecoder;
import com.chatalytics.core.realtime.ChatAlyticsEventEncoder;
import com.chatalytics.core.realtime.ConnectionTypeEncoderDecoder;
import com.google.common.base.Throwables;

import org.apache.storm.shade.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * This resource will pipe {@link ChatAlyticsEvent}s received from the compute server to any clients
 * listening for realtime events using sockets. The compute client and this resource clients are
 * sharing a set of all the open client sessions
 *
 * @author giannis
 */
@ServerEndpoint(value = EventsResource.RT_EVENT_ENDPOINT,
                decoders = { ChatAlyticsEventDecoder.class },
                encoders = { ChatAlyticsEventEncoder.class })
@ClientEndpoint(decoders = { ChatAlyticsEventDecoder.class, ConnectionTypeEncoderDecoder.class })
public class EventsResource {

    public static final String RT_EVENT_ENDPOINT = "/events";
    private static final Logger LOG = LoggerFactory.getLogger(EventsResource.class);

    private final Set<Session> sessions;

    public EventsResource() {
        this.sessions = Sets.newConcurrentHashSet();
    }

    @OnOpen
    public void onOpen(Session session) {
        if (session.getRequestURI().getPath().startsWith(RT_EVENT_ENDPOINT)) {
            LOG.info("Got a new web subscription connection request with ID {}", session.getId());

            // cleanup sessions
            Set<Session> closedSessions = Sets.newHashSet();
            for (Session existingSession : sessions) {
                if (!existingSession.isOpen()) {
                    closedSessions.add(existingSession);
                }
            }
            sessions.removeAll(closedSessions);

            sessions.add(session);
        } else {
            LOG.info("Handshaked with compute server...");
        }
    }

    /**
     * Closes a session
     *
     * @param session
     *            The session to close
     * @param reason
     *            The reason for closing
     */
    @OnClose
    public void close(Session session, CloseReason reason) {
        LOG.info("Closing session {}. Reason {}", session.getId(), reason);
        try {
            session.close();
            sessions.remove(session);
        } catch (IOException e) {
            LOG.warn("Couldn't close {}", session.getId());
        }
    }

    /**
     * Called whenever a new event is received from the compute socket
     *
     * @param event
     *            The triggering event
     */
    @OnMessage
    public void onMessage(ChatAlyticsEvent event) {

        LOG.debug("Got realtime event: {}", event);

        Set<Session> closedSessions = Sets.newHashSet();
        for (Session clientSession : sessions) {
            if (!clientSession.isOpen()) {
                closedSessions.add(clientSession);
                continue;
            }

            // don't expose package info to client
            event.setClazz(null);
            clientSession.getAsyncRemote().sendObject(event);
        }

        sessions.removeAll(closedSessions);
    }

    /**
     * Called whenever an exception occurs while the websocket session is active
     *
     * @param t
     *            The exception
     */
    @OnError
    public void onError(Throwable t) {
        LOG.error(Throwables.getStackTraceAsString(t));
    }
}
