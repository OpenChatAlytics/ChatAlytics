package com.chatalytics.compute.storm.spout;

import com.chatalytics.compute.chat.dao.ChatAPIFactory;
import com.chatalytics.compute.chat.dao.IChatApiDAO;
import com.chatalytics.compute.chat.dao.slack.JsonSlackDAO;
import com.chatalytics.compute.config.ConfigurationConstants;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.config.SlackConfig;
import com.chatalytics.core.model.data.FatMessage;
import com.chatalytics.core.model.data.Message;
import com.chatalytics.core.model.data.MessageType;
import com.chatalytics.core.model.data.Room;
import com.chatalytics.core.model.data.User;
import com.chatalytics.core.util.YamlUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.glassfish.tyrus.container.jdk.client.JdkContainerProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Spout that pulls messages from the slack API and emits {@link FatMessage}s to subscribed bolts.
 *
 * @author giannis
 */
@ClientEndpoint(decoders = { WebSocketMessageDecoder.class })
public class SlackMessageSpout extends BaseRichSpout {

    private static final long serialVersionUID = -6294446748544704853L;
    private static final Logger LOG = LoggerFactory.getLogger(SlackMessageSpout.class);
    public static final String SPOUT_ID = "SLACK_MESSAGE_SPOUT_ID";
    public static final String SLACK_MESSAGE_FIELD_STR = "slack-message";

    private IChatApiDAO slackDao;
    private SpoutOutputCollector collector;

    private final ConcurrentLinkedQueue<FatMessage> unemittedMessages;
    private Session session;
    private Optional<DateTime> startDate;

    public SlackMessageSpout() {
        unemittedMessages = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void open(@SuppressWarnings("rawtypes") Map conf, TopologyContext context,
                     SpoutOutputCollector collector) {
        String configYaml = (String) conf.get(ConfigurationConstants.CHATALYTICS_CONFIG.txt);
        ChatAlyticsConfig config = YamlUtils.readChatAlyticsConfigFromString(configYaml);

        LOG.info("Loaded config...");
        WebSocketContainer webSocketContainer = JdkContainerProvider.getWebSocketContainer();
        IChatApiDAO slackDao = ChatAPIFactory.getChatApiDao(config);
        SlackConfig slackConfig = (SlackConfig) config.computeConfig.chatConfig;
        open(slackConfig, slackDao, webSocketContainer, context, collector);
    }

    @VisibleForTesting
    protected void open(SlackConfig slackConfig, IChatApiDAO slackDao,
                        WebSocketContainer webSocketContainer, TopologyContext context,
                        SpoutOutputCollector collector) {
        this.slackDao = slackDao;
        this.collector = collector;

        String startDateNullable = slackConfig.startDate;
        // get end date, if there is one
        if (startDateNullable != null) {
            this.startDate = Optional.of(DateTime.parse(startDateNullable));
        } else {
            this.startDate = Optional.absent();
        }

        openRealtimeConnection(slackConfig, webSocketContainer);
    }

    /**
     * Tries to initiate the realtime connection with retries
     *
     * @param slackConfig The slack config
     * @param webSocketContainer The web socket container to connect with
     */
    private void openRealtimeConnection(SlackConfig slackConfig,
                                          WebSocketContainer webSocketContainer) {
        boolean connected = false;
        int retryCount = 0;
        int sleepIntervalMs = slackConfig.sourceConnectionSleepIntervalMs;
        int retryBackoffMaxSleepMs = slackConfig.sourceConnectionBackoffMaxSleepMs;
        int globalMaxMs = slackConfig.sourceConnectionMaxMs;
        long connectionStartTimeMs = System.currentTimeMillis();

        URI webSocketUri = null;
        while (!connected) {
            try {
                webSocketUri = getRealtimeWebSocketURI();
                session = webSocketContainer.connectToServer(this, webSocketUri);
                connected = true;
                LOG.info("RTM session created with id {}", session.getId());
            } catch (Exception e) {
                LOG.error("Unable to connect to {}. Retrying {}...", webSocketUri, ++retryCount, e);
                long timePassedMs = System.currentTimeMillis() - connectionStartTimeMs;
                if (timePassedMs > globalMaxMs) {
                    throw new RuntimeException("Can't connect to " + webSocketUri, e);
                }
                int sleepTimeMs = Math.min(retryBackoffMaxSleepMs, retryCount * sleepIntervalMs);
                long timeLeft = globalMaxMs-timePassedMs;
                LOG.info("Sleeping for {}ms. {}ms before giving up", sleepTimeMs, timeLeft);
                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ie) {
                    LOG.error("Interrupted while sleeping...", ie);
                }
            }
        }
    }

    /**
     * Gets the realtime web socket URI first by checking to see if the current implementation of
     * the {@link IChatApiDAO} supports this
     *
     * @return The web socket URI to connect to the realtime slack message stream
     */
    protected URI getRealtimeWebSocketURI() {
        return ((JsonSlackDAO) slackDao).getRealtimeWebSocketURI();
    }

    /**
     * Called when a new chat message event is received. A {@link FatMessage} is created and pushed
     * to a concurrent queue for consumption.
     *
     * @param message
     *            The message event
     * @param session
     *            The active websocket session
     */
    @OnMessage
    public void onMessageEvent(Message message, Session session) {
        LOG.debug("Got event {}", message);

        if (filterMessage(message)) {
            LOG.debug("Filtering message dated {}", message.getDate());
            return;
        }

        Map<String, User> users = slackDao.getUsers();
        Map<String, Room> rooms = slackDao.getRooms();

        User fromUser = users.get(message.getFromUserId());
        if (fromUser == null && message.getType() == MessageType.BOT_MESSAGE) {
            fromUser = new User(message.getFromUserId(), null, false, false, true,
                                message.getFromName(), message.getFromName(), null, DateTime.now(),
                                null, null, null, null, null);
        }

        if (fromUser == null) {
            LOG.warn("Can't find user with userId: {}. Skipping", message.getFromUserId());
            return;
        }

        Room room = rooms.get(message.getRoomId());
        if (room == null && message.getRoomId() != null) {
            room = new Room(message.getRoomId(), message.getRoomId(), null,
                            DateTime.now(DateTimeZone.UTC), null, null, false, true, null, null);
        }
        FatMessage fatMessage = new FatMessage(message, fromUser, room);
        unemittedMessages.add(fatMessage);
    }

    /**
     * Filters messages that dont meet certain criteria. Right now the only reason why a message
     * would get filtered is if it occurred before the configured start date
     *
     * @param message
     *            The message to inspect and potentially filter
     *
     * @return True if the message should get filtered, false otherwise
     */
    private boolean filterMessage(Message message) {
        if (!startDate.isPresent()) {
            return false;
        }
        DateTime messageDate = message.getDate();
        if (startDate.get().equals(messageDate) || startDate.get().isAfter(messageDate)) {
            return true;
        } else {
            return false;
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

    /**
     * Consumes from a queue that is populated by the {@link #onMessageEvent(Message, Session)}
     * method
     */
    @Override
    public void nextTuple() {
        while (!unemittedMessages.isEmpty()) {
            FatMessage fatMessage = unemittedMessages.remove();
            collector.emit(new Values(fatMessage));
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer fields) {
        fields.declare(new Fields(SLACK_MESSAGE_FIELD_STR));
    }

    @Override
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                LOG.error("Session did not close cleanly. Got {}", e.getMessage());
            }
        }
    }

}
