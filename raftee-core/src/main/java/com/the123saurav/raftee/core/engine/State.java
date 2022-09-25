package com.the123saurav.raftee.core.engine;

/**
 * State indicates state persisted by a node on disk.
 */
public final class State {
    private long term;
    private String votedForNode;
}
