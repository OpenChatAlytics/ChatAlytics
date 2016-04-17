package com.chatalytics.compute.storm;

import com.chatalytics.compute.ChatAlyticsEngineMain;
import com.chatalytics.compute.config.ConfigurationConstants;
import com.chatalytics.compute.realtime.ComputeRealtimeServer;
import com.chatalytics.compute.util.YamlUtils;
import com.chatalytics.core.config.ChatAlyticsConfig;
import com.google.common.util.concurrent.AbstractIdleService;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;

/**
 * Service that configures the storm topology and then starts it up. This is started by
 * {@link ChatAlyticsEngineMain}.
 *
 * @author giannis
 *
 */
public class ChatAlyticsService extends AbstractIdleService {

    private final StormTopology chatTopology;
    private LocalCluster cluster;
    private final ChatAlyticsConfig chatalyticsConfig;
    private final ComputeRealtimeServer rtServer;

    public ChatAlyticsService(StormTopology chatTopology,
                              ComputeRealtimeServer rtServer,
                              ChatAlyticsConfig chatalyticsConfig) {
        this.chatTopology = chatTopology;
        this.chatalyticsConfig = chatalyticsConfig;
        this.rtServer = rtServer;
    }

    private LocalCluster submitTopology() throws AlreadyAliveException,
            InvalidTopologyException {
        Config stormConfig = new Config();
        stormConfig.setDebug(false);
        stormConfig.setFallBackOnJavaSerialization(true);
        stormConfig.setNumWorkers(1);
        stormConfig.setSkipMissingKryoRegistrations(true);
        stormConfig.put(ConfigurationConstants.CHATALYTICS_CONFIG.txt,
                        YamlUtils.writeYaml(chatalyticsConfig));

        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("chat-topology", stormConfig, chatTopology);
        return cluster;
    }

    @Override
    protected void startUp() throws Exception {
        rtServer.startAsync().awaitRunning();
        cluster = submitTopology();
    }

    @Override
    protected void shutDown() throws Exception {
        cluster.shutdown();
        rtServer.stopAsync().awaitTerminated();
    }
}
