package com.the123saurav.raftee.core.engine;

import com.the123saurav.raftee.api.RaftConfig;
import com.the123saurav.raftee.api.RaftEngine;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SimpleRaftEngine implements RaftEngine {
    public SimpleRaftEngine(RaftConfig config) {
        log.info("In SimpleRaftEngine with config {}", config);

    }
}
