package com.the123saurav.raftee.demoapp;

import com.the123saurav.raftee.api.RaftConfig;
import com.the123saurav.raftee.core.RaftEngineProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        RaftConfig cfg = RaftConfig.builder()
                .clusterName("foo")
                .clusterNodes(new HashSet<>(List.of("5c523099b12d.ant.amazon.com:4567")))
                .dataDir("/tmp")
                .build();
        RaftEngineProvider.of(cfg);
    }
}
