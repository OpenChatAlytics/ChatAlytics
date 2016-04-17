package com.chatalytics.core.config;

import com.chatalytics.core.InputSourceType;

import java.io.Serializable;
import java.util.Map;

/**
 * The configuration object. Fields of this object are serialized and put in the storm configuration
 * map object. The fields in this object are public and are set through a YAML file found in the
 * resources path.
 *
 * @author giannis
 *
 */
public class ChatAlyticsConfig implements Serializable {

    private static final long serialVersionUID = -1251758543444208166L;

    public InputSourceType inputType;

    public String apiDateFormat;

    public String timeZone = "America/New_York";

    public int apiRetries = 3;

    public String classifier = "classifiers/english.all.3class.distsim.crf.ser.gz";

    public String persistenceUnitName = "chatalytics-db";

    public final String rtComputePath = "/rtcompute";

    public int rtComputePort = 9000;

    public HipChatConfig hipchatConfig = new HipChatConfig();

    public SlackConfig slackConfig = new SlackConfig();

    public LocalTestConfig localTestConfig = new LocalTestConfig();

    public BackfillerConfig backfillerConfig = new BackfillerConfig();

    /**
     * Map of property name to file to read
     */
    public Map<String, String> filesToRead;

}
