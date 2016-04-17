package com.chatalytics.compute.storm.bolt;

import com.chatalytics.compute.config.ConfigurationConstants;
import com.chatalytics.compute.db.dao.ChatAlyticsDAOFactory;
import com.chatalytics.compute.db.dao.IEntityDAO;
import com.chatalytics.compute.util.YamlUtils;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.model.ChatEntity;
import com.chatalytics.core.model.FatMessage;
import com.chatalytics.core.model.Message;
import com.chatalytics.core.model.Room;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.XMLUtils;
import edu.stanford.nlp.util.XMLUtils.XMLTag;

import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * This bolt received one message at a time, parses the text of the message and extract entities.
 * Once the entities are extracted and a {@link ChatEntity} object is created, it's persisted to a
 * database.
 *
 * @author giannis
 *
 */
public class EntityExtractionBolt extends BaseRichBolt {

    private static final long serialVersionUID = -1586393277809132608L;
    private static final Logger LOG = LoggerFactory.getLogger(EntityExtractionBolt.class);

    public static final String BOLT_ID = "ENTITY_EXTRACTION_BOLT_ID";
    public static final String CHAT_ENTITY_FIELD_STR = "chat-entity";
    private static final int MAX_ENTITY_CHARS = 150;

    private AbstractSequenceClassifier<CoreLabel> classifier;
    private IEntityDAO entityDao;
    private OutputCollector collector;

    @Override
    public void prepare(@SuppressWarnings("rawtypes") Map conf, TopologyContext context,
                        OutputCollector collector) {
        String configStr = (String) conf.get(ConfigurationConstants.CHATALYTICS_CONFIG.txt);
        ChatAlyticsConfig config = YamlUtils.readYamlFromString(configStr, ChatAlyticsConfig.class);
        classifier = getClassifier(config);
        entityDao = ChatAlyticsDAOFactory.getEntityDAO(config);
        if (!entityDao.isRunning()) {
            entityDao.startAsync().awaitRunning();
        }
        this.collector = collector;
    }

    /**
     * Gets the classifier to use for parsing text
     *
     * @param config
     *            The configuration object containing information about which classifier to use.
     * @return The classifier to use for extracting entities.
     */
    private AbstractSequenceClassifier<CoreLabel> getClassifier(ChatAlyticsConfig config) {
        URL classifierURL = Resources.getResource(config.classifier);
        AbstractSequenceClassifier<CoreLabel> classifier =
            CRFClassifier.getClassifierNoExceptions(classifierURL.getPath());
        return classifier;
    }

    @Override
    public void execute(Tuple input) {
        LOG.info("Got tuple: {}", input);
        FatMessage fatMessage = (FatMessage) input.getValue(0);
        if (fatMessage == null) {
            LOG.warn("Got a null tuple");
            return;
        }

        List<ChatEntity> entities = extractEntities(fatMessage);
        for (ChatEntity entity : entities) {
            entityDao.persistEntity(entity);
            collector.emit(new Values(entity));
        }
    }

    /**
     * Given a message this method uses a classifier to extract entities.
     *
     * @param fatMessage
     *            The message containing the text to parse.
     * @return A list of entities from the text
     */
    @VisibleForTesting
    protected List<ChatEntity> extractEntities(FatMessage fatMessage) {
        Message message = fatMessage.getMessage();
        Map<String, ChatEntity> entities = Maps.newHashMap();
        String messageWithXML = classifier.classifyWithInlineXML(message.getMessage());
        Reader r = new StringReader(messageWithXML);

        try {
            while (true) {
                XMLTag tag = XMLUtils.readAndParseTag(r);
                if (tag == null || tag.name.length() == 0) {
                    break;
                }

                String entity = XMLUtils.readUntilTag(r);

                if (entity.length() > MAX_ENTITY_CHARS) {
                    continue;
                }

                if (!tag.isEndTag) {
                    ChatEntity existingEntity = entities.remove(entity);
                    int occurrences;
                    if (existingEntity == null) {
                        occurrences = 1;
                    } else {
                        occurrences = existingEntity.getOccurrences() + 1;
                    }
                    Room room = fatMessage.getRoom();
                    String roomName;
                    if (room == null) {
                        roomName = "";
                    } else {
                        roomName = room.getName();
                    }
                    entities.put(entity, new ChatEntity(entity,
                                                        occurrences,
                                                        message.getDate(),
                                                        fatMessage.getUser().getMentionName(),
                                                        roomName));
                }
            }

        } catch (Exception e) {
            LOG.error("Could not extract entity. Ignoring...", e);
        }

        LOG.debug("Extracted {} entities", entities.size());

        return Lists.newArrayList(entities.values());
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer fields) {
        fields.declare(new Fields(CHAT_ENTITY_FIELD_STR));
    }

    @Override
    public void cleanup() {
        entityDao.stopAsync().awaitTerminated();
    }

}
