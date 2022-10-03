package com.the123saurav.raftee.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Builder
@ToString
@Getter
public class RaftConfig {
    private final String clusterName;
    private final Set<InetSocketAddress> clusterNodes;
    private final Path dataDir;
    private final int connectTimeoutMs;
    private final int socketTimeoutMs;

    /**
     * Uses custom builder class
     */
    public static RaftConfigBuilder builder() {
        return new CopyRaftConfigBuilder();
    }

    /**
     * Custom builder class
     */
    private static class CopyRaftConfigBuilder extends RaftConfigBuilder {
        @Override
        public RaftConfig build() {
            try {
                Validate.notBlank(super.clusterName, "ClusterName '%s' can not be blank", super.clusterName);

                Validate.noNullElements(super.clusterNodes, "ClusterName '%s' can not be blank", super.clusterName);
                super.clusterNodes.forEach(cn -> {
                    Validate.notNull(cn, "ClusterNode element '%s' can not be blank", cn);
                    Validate.notBlank(cn.getHostName(), "ClusterNode element '%s' host is not valid", cn);
                    Validate.isTrue(cn.getPort() != -1, "ClusterNode element '%s' port is not valid", cn);
                });

                Validate.isTrue(Files.isWritable(super.dataDir) && Files.isReadable(super.dataDir),
                        "Data directory %s is not readable/writable", super.dataDir);

                Validate.isTrue(super.connectTimeoutMs > 0, "Connect timeout must be set, currently: "
                + super.connectTimeoutMs);

                Validate.isTrue(super.socketTimeoutMs > 0, "Socket timeout must be set, currently: "
                        + super.socketTimeoutMs);
            } catch (NullPointerException | IllegalArgumentException ex){
                throw new BadConfigException("bad raft config", ex);
            }

            return super.build();
        }
    }

    @Override
    public RaftConfig clone() {
        return new RaftConfig(clusterName, new HashSet<>(clusterNodes), dataDir, connectTimeoutMs, socketTimeoutMs);
    }
}
