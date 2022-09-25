package com.the123saurav.raftee.core.network;


import com.the123saurav.raftee.api.RaftConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private static Map<ClusterEndpoint, SocketChannel> connections = null;
    private final RaftConfig raftConfig;

    private ConnectionManager(List<ClusterEndpoint> endpoints, RaftConfig raftCfg) {
        connections = new ConcurrentHashMap<>(endpoints.size());
        endpoints.forEach(ep -> connections.put(ep, null));
        raftConfig = raftCfg;
    }

    // TODO: think if we need below method
    public static ConnectionManager from(List<ClusterEndpoint> endpoints, RaftConfig raftCfg) {
        return new ConnectionManager(endpoints, raftCfg);
    }

    /**
     * Returns an existing connection(either we created or we got from other) or creates one.
     * @param endpoint
     * @return
     * @throws IOException
     */
    public SocketChannel getBlockingConnection(ClusterEndpoint endpoint) throws IOException {
        try {
            return connections.computeIfAbsent(endpoint, (ep) -> {
                try {
                    InetSocketAddress socketAddress = new InetSocketAddress(ep.endpoint().getHost(), ep.endpoint().getPort());
                    SocketChannel ch = SocketChannel.open();
                    ch.configureBlocking(true);
                    ch.socket().setSoTimeout(raftConfig.getSocketTimeoutMs());
                    ch.socket().connect(socketAddress, raftConfig.getConnectTimeoutMs());
                    ch.configureBlocking(false);
                    return ch;
                } catch (IOException e) {
                    throw new RuntimeException("Unable to connect to:" + endpoint.endpoint(), e);
                }
            });
        } catch (final RuntimeException ex) {
            if (ex.getCause() instanceof IOException e) {
                throw e;
            }
            throw ex;
        }
    }

    /**
     * Allows setting a connection from outside, for example leader got a JoinCluster from follower.
     * @param ep
     * @param channel
     * @return
     */
    public boolean setConnection(ClusterEndpoint ep, SocketChannel channel) {
        return connections.putIfAbsent(ep, channel) == null;
    }
}
