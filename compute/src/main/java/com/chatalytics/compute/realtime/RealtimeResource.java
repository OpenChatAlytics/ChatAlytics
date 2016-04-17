package com.chatalytics.compute.realtime;

import com.chatalytics.core.model.ChatAlyticsEvent;
import com.chatalytics.core.realtime.ChatAlyticsEventDecoder;
import com.chatalytics.core.realtime.ChatAlyticsEventEncoder;
import com.chatalytics.core.realtime.ConnectionType;
import com.chatalytics.core.realtime.ConnectionTypeEncoderDecoder;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * Resource for the bolts to post realtime chatalytics and for the web server to connect to and get
 * the stream of chatalytics
 *
 * @author giannis
 *
 */
@ServerEndpoint(value = RealtimeResource.RT_FULL_ENDPOINT,
                encoders = { ChatAlyticsEventEncoder.class, ConnectionTypeEncoderDecoder.class },
                decoders = { ChatAlyticsEventDecoder.class, ConnectionTypeEncoderDecoder.class })
public class RealtimeResource {

    public static final String RT_COMPUTE_ENDPOINT = "/rtcompute";
    private static final String RT_COMPUTE_ENDPOINT_PARAMETER = "type";
    public static final String RT_FULL_ENDPOINT =
        RT_COMPUTE_ENDPOINT + "/{" + RT_COMPUTE_ENDPOINT_PARAMETER + "}";

    private static final Logger LOG = LoggerFactory.getLogger(RealtimeResource.class);

    private static Set<Session> sessions;

    public RealtimeResource() {
        sessions = Sets.newConcurrentHashSet();
    }

    /**
     * Open a socket connection to a client from the web server
     *
     * @param session The session that just opened
     */
    @OnOpen
    public void openSocket(@PathParam(RT_COMPUTE_ENDPOINT_PARAMETER) ConnectionType type,
                           Session session) {
        if (type == ConnectionType.SUBSCRIBER) {
            LOG.info("Got a new subscriber connection request with ID {}. Saving session",
                     session.getId());

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
            LOG.info("Got a new publisher connection request with ID {}", session.getId());
        }
    }

    @OnMessage
    public void publishEvent(ChatAlyticsEvent event) {
        Set<Session> closedSessions = Sets.newHashSet();
        for (Session session : sessions) {
            if (!session.isOpen()) {
                closedSessions.add(session);
                continue;
            }
            session.getAsyncRemote().sendObject(event);
        }

        sessions.removeAll(closedSessions);
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
