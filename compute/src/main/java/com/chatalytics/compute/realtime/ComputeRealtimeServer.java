package com.chatalytics.compute.realtime;

import com.google.common.util.concurrent.AbstractIdleService;

import org.eclipse.jetty.server.Server;

/**
 * Realtime server where client can connect to in order to retrieve realtime chatalytics
 *
 * @author giannis
 *
 */
public class ComputeRealtimeServer extends AbstractIdleService {

    private final Server server;

    public ComputeRealtimeServer(Server server) {
        this.server = server;
    }

    @Override
    protected void startUp() throws Exception {
        server.start();
    }

    @Override
    protected void shutDown() throws Exception {
        server.stop();
    }

}
