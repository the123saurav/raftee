package com.the123saurav.raftee.core.engine;

import com.the123saurav.raftee.api.RaftConfig;
import lombok.Builder;

@Builder
public class Config {
    @Builder
    static class NetworkConfig {

    }

    private final RaftConfig clientConfig;



}
