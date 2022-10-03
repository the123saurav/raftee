package com.the123saurav.raftee.demoapp;

import com.the123saurav.raftee.api.RaftConfig;
import com.the123saurav.raftee.api.RaftEngine;
import com.the123saurav.raftee.core.RaftEngineProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, InterruptedException {
        RaftConfig cfg = RaftConfig.builder()
                .clusterName("foo")
                .clusterNodes(new HashSet<>(List.of(InetSocketAddress.createUnresolved("5c523099b12d.ant.amazon.com", 4567))))
                .dataDir(Path.of("/tmp/raftee"))
                .connectTimeoutMs(1000)
                .socketTimeoutMs(1000)
                .build();
        RaftEngine engine = RaftEngineProvider.of(cfg);
        engine.start();
        Thread.sleep(10000);
    }
}
