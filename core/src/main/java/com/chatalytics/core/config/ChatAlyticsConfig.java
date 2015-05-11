package com.chatalytics.core.config;

import java.io.Serializable;

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

    public String apiDateFormat;

    public String timeZone = "America/New_York";

    public int apiRetries = 3;

    public String classifier = "classifiers/english.all.3class.distsim.crf.ser.gz";

    public String persistenceUnitName = "chatalytics-db";

    public HipChatConfig hipchatConfig;

}
