package com.the123saurav.raftee.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * FSM is the way to interface Raft engine
 * with client. Raft engine provides the durably committed data to
 * FSM so that client can apply it.
 */
public interface AppFSM {

    public static class Record {
        byte[] command;
    }

    /**
     * Apply a list of committed records.
     * @param record Record
     */
    void apply(List<Record> record);

    /**
     * Snapshot the current applied state machine and write to OytputStream.
     * @param outputStream The stream to write snapshot
     * @return Returns success/failure asynchronously
     */
    CompletableFuture<Boolean> snapshot(OutputStream outputStream);

    /**
     * Replay a snapshot to construct initial state
     * @param inputStream Stream to read from
     * @return Returns success/failure asynchronously
     */
    CompletableFuture<Boolean> replay(InputStream inputStream);
}
