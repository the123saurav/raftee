package com.the123saurav.raftee.core.persistence.model;

import com.the123saurav.raftee.core.persistence.Serializable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * An IndexOffset entry in persistent store tracks max index for a term and its file offset.
 * The entry is fixed in size:
 * 8 + 8 + 8 + 4 = 28B
 */
public record IndexOffset(long term, long index, long offset, int checksum) implements Serializable {

    @Override
    public void serialize(ByteBuffer buffer) {
        buffer.putLong(term);
        buffer.putLong(index);
        buffer.putLong(offset);
        buffer.putInt(checksum);
    }

    public static List<IndexOffset> from(ByteBuffer buffer) {
        short recordSize = 28;
        List<IndexOffset> records = new ArrayList<>(buffer.remaining() / recordSize);
        while (buffer.remaining() >= recordSize) {
            records.add(new IndexOffset(buffer.getLong(), buffer.getLong(), buffer.getLong(), buffer.getInt()));
        }
        return records;
    }
}
