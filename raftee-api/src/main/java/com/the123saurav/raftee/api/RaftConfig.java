package com.the123saurav.raftee.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Builder
@ToString
@Getter
public class RaftConfig {
    private final String clusterName;
    private final Set<String> clusterNodes;
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
                    Validate.notBlank(cn, "ClusterNode element '%s' can not be blank", cn);
                    try {
                        URI nodeURI = new URI("raft://" + cn);
                        Validate.notBlank(nodeURI.getHost(), "ClusterNode element '%s' is not valid", cn);
                        Validate.isTrue(nodeURI.getPort() != -1, "ClusterNode element '%s' is not valid", cn);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException(String.format("ClusterNode element '%s' is not valid", cn));
                    }
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
