package com.the123saurav.raftee.core;

import com.the123saurav.raftee.api.BadConfigException;
import com.the123saurav.raftee.api.RaftConfig;
import com.the123saurav.raftee.api.RaftEngine;
import com.the123saurav.raftee.core.engine.SimpleRaftEngine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class RaftEngineProvider {

    public static RaftEngine of(RaftConfig config) throws IOException {
        boolean isValid = config.getClusterNodes().stream()
                .map(cn -> {
                    try {
                        return new URI("raft://" + cn).getHost();
                    } catch (URISyntaxException e) {
                        // Unreachable as we already check in RaftConfig but handling, if this is modified out of band.
                        throw new BadConfigException("Error in reading hostname from RaftConfig", e);
                    }
                })
                .anyMatch(h -> {
                    try {
                        return h.equals(execCmd("hostname"));
                    } catch (IOException e) {
                        throw new RuntimeException("Error while getting local hostname", e);
                    }
                });
        if (!isValid) {
            throw new IllegalStateException("Current host is not in cluster config");
        }
        RaftConfig cfg = config.clone();

        return new SimpleRaftEngine(cfg);
    }

    private static String execCmd(String cmd) throws IOException {
        String result;
        try (InputStream inputStream = Runtime.getRuntime().exec(cmd).getInputStream();
             Scanner s = new Scanner(inputStream).useDelimiter("\\n")) {
            result = s.hasNext() ? s.next() : null;
        }
        return result;
    }
}
