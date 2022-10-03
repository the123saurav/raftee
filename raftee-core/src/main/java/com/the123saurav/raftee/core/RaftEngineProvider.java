package com.the123saurav.raftee.core;

import com.the123saurav.raftee.api.RaftConfig;
import com.the123saurav.raftee.api.RaftEngine;
import com.the123saurav.raftee.core.engine.SimpleRaftEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftEngineProvider {

    public static RaftEngine of(RaftConfig config) throws IOException {
        AtomicInteger port = new AtomicInteger(-1);
        boolean isValid = config.getClusterNodes().stream()
                .anyMatch(cn -> {
                    try {
                        port.set(cn.getPort());
                        return cn.getHostName().equals(execCmd("hostname"));
                    } catch (IOException e) {
                        throw new RuntimeException("Error while getting local hostname", e);
                    }
                });
        if (!isValid) {
            throw new IllegalStateException("Current host is not in cluster config");
        }
        RaftConfig cfg = config.clone();

        return new SimpleRaftEngine(cfg, port.get());
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
