package com.the123saurav.raftee.core.persistence.model;

import com.the123saurav.raftee.core.persistence.Serializable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry represents a single record in log.
 * The format is
 * <term> <index> <datLen> <data> <checksum> <ts>
 * 8     + 8     + 4      +   N  +   4      + 8 = 32 + N
 * This format allows good enough forward traversal but not backwards.
 */
public record Log(long term, long index, byte[] data, int checksum, long ts) implements Serializable {

    @Override
    public void serialize(ByteBuffer buf) {
        buf.putLong(term);
        buf.putLong(index);
        buf.putInt(data.length);
        buf.put(data);
        buf.putInt(checksum);
        buf.putLong(ts);
    }

    public static List<Log> from(ByteBuffer buffer){
        return from(buffer, Long.MAX_VALUE);
    }

    public static List<Log> from(ByteBuffer buffer, long numLogs) {
        int curr = 0;
        short recordSize = 32 + 20;
        // TODO check if we can reuse below
        List<Log> records = new ArrayList<>(buffer.remaining() / recordSize);
        while (curr++ != numLogs && buffer.remaining() > 32) {
            buffer.mark();
            long term = buffer.getLong();
            long index = buffer.getLong();
            int dataLen = buffer.getInt();
            if (buffer.remaining() < 32 + dataLen) {
                buffer.reset();
                break;
            }
            byte[] data = new byte[dataLen];
            buffer.get(data);
            records.add(new Log(term, index, data, buffer.getInt(), buffer.getLong()));
        }
        buffer.reset();
        return records;
    }
}
