package com.the123saurav.raftee.core.persistence.log;

/**
 * Entry represents a single record in log.
 * The format is
 * <term> <index> <datLen> <data> <checksum> <ts>
 * 4     + 8     + 4      +   N  +   4      + 8 = 28 + N
 * This format allows good enough forward traversal but not backwards.
 */
public class Entry {
    int term;
    long index;
    int dataLen;
    byte[] data;
    int checksum;
    long ts;
}
