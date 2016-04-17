package com.chatalytics.compute.storm.bolt;

import com.chatalytics.compute.config.ConfigurationConstants;
import com.chatalytics.compute.db.dao.ChatAlyticsDAOFactory;
import com.chatalytics.compute.db.dao.IEmojiDAO;
import com.chatalytics.compute.util.YamlUtils;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.chatalytics.core.model.EmojiEntity;
import com.chatalytics.core.model.FatMessage;
import com.chatalytics.core.model.Room;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import org.apache.storm.shade.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfInt;

public class EmojiCounterBolt extends BaseRichBolt {

    private static final long serialVersionUID = -3543087188985057557L;
    public static final String BOLT_ID = "EMOJI_COUNTER_BOLT_ID";
    public static final String EMOJI_ENTITY_FIELD_STR = "emoji-entity";
    private static final Logger LOG = LoggerFactory.getLogger(EmojiCounterBolt.class);

    private IEmojiDAO emojiDao;
    private OutputCollector collector;

    @Override
    public void prepare(@SuppressWarnings("rawtypes") Map conf, TopologyContext context,
                        OutputCollector collector) {
        String configStr = (String) conf.get(ConfigurationConstants.CHATALYTICS_CONFIG.txt);
        ChatAlyticsConfig config = YamlUtils.readYamlFromString(configStr, ChatAlyticsConfig.class);
        emojiDao = ChatAlyticsDAOFactory.getEmojiDAO(config);
        this.collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        LOG.info("Got tuple: {}", input);
        FatMessage fatMessage = (FatMessage) input.getValue(0);
        if (fatMessage == null) {
            LOG.warn("Got a null tuple");
            return;
        }

        List<EmojiEntity> emojis = getEmojisFromMessage(fatMessage);

        for (EmojiEntity emoji : emojis) {
            emojiDao.persistEmoji(emoji);
            collector.emit(new Values(emoji));
        }
    }

    @VisibleForTesting
    protected List<EmojiEntity> getEmojisFromMessage(FatMessage fatMessage) {
        String message = fatMessage.getMessage().getMessage();
        Map<String, EmojiEntity> emojis = Maps.newHashMap();

        OfInt charIterator = message.chars().iterator();

        Room room = fatMessage.getRoom();
        String roomName;
        if (room == null) {
            roomName = "";
        } else {
            roomName = room.getName();
        }

        boolean capturingEmoji = false;
        StringBuilder emojiStrBuilder = new StringBuilder();

        while (charIterator.hasNext()) {
            int asciiValue = charIterator.next();
            char ch = (char) asciiValue;
            if (ch == ':') {
                // done capturing
                if (capturingEmoji) {

                    String emoji = emojiStrBuilder.toString();

                    EmojiEntity existingEmoji = emojis.remove(emoji);
                    int occurrences;
                    if (existingEmoji == null) {
                        occurrences = 1;
                    } else {
                        occurrences = existingEmoji.getOccurrences() + 1;
                    }

                    emojis.put(emoji, new EmojiEntity(emoji,
                                                      occurrences,
                                                      fatMessage.getMessage().getDate(),
                                                      fatMessage.getUser().getMentionName(),
                                                      roomName));
                    emojiStrBuilder = new StringBuilder();
                }
                capturingEmoji = !capturingEmoji;
                continue;
            }
            if (capturingEmoji) {

                if (ch == ' ' || ch == '\t') {
                    capturingEmoji = !capturingEmoji;
                    emojiStrBuilder = new StringBuilder();
                    continue;
                }

                emojiStrBuilder.append(ch);
            }
        }

        LOG.debug("Extracted {} emojis", emojis.size());

        return Lists.newArrayList(emojis.values());
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer fields) {
        fields.declare(new Fields(EMOJI_ENTITY_FIELD_STR));
    }

}
