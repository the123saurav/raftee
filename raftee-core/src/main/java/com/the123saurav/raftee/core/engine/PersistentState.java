package com.the123saurav.raftee.core.engine;

/**
 * State indicates state persisted by a node on disk.
 */
public final class PersistentState {
    private long term;
    private String votedForNode;
}
