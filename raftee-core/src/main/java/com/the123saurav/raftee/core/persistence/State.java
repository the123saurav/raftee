package com.the123saurav.raftee.core.persistence;

/**
 * State indicates state persisted by a node on disk.
 */
public record State(long term, String votedForNode) {
}
