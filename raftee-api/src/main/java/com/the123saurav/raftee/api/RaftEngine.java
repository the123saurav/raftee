package com.the123saurav.raftee.api;

import java.util.concurrent.CompletableFuture;

/**
 * RaftEngine is the core raft's engine interface.
 */
public interface RaftEngine {
    /**
     * Asynchronously starts the raft engine.
     * Throws if start has failure.
     * Callers should wait for the future to complete before
     * assuming normal operations.
     * @return
     */
    CompletableFuture<Void> start();

    CompletableFuture<Void> stop();
}
