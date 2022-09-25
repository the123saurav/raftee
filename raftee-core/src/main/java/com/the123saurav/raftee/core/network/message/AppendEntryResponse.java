package com.the123saurav.raftee.core.network.message;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * joinResult - true if this node is accepted post index match, false in case
 * current node is not leader OR the index match has not happened. In case of latter, leader sends
 * @param term and
 * @param index for node to know what leader has
 */
public record AppendEntryResponse(FollowerState followerState, long term, long index, Error error) implements Message {

    public static AppendEntryResponse from(ByteBuffer buffer, short msgSz) {
        buffer.mark();
        try {
            FollowerState followerState = FollowerState.from(buffer.get());
            long term = buffer.getLong();
            long index = buffer.getLong();
            short errSz = (short) (msgSz - 17);
            Error error = null;
            if (errSz > 0) {
                error = Error.from(buffer, errSz);
                if (error == null) {
                    throw new RuntimeException(String.format("Unable to read error from socket, bytes: %d, errSz: %d",
                            msgSz, errSz));
                }
            }
            return new AppendEntryResponse(followerState, term, index, error);
        } catch (BufferUnderflowException ex) {
            buffer.reset();
            return null;
        }
    }

    @Override
    public void serialize(ByteBuffer buf) {
        buf.mark();
        try {
            buf.put(followerState.serialize());
            buf.putLong(term);
            buf.putLong(index);
            if (error != null) {
                error.serialize(buf);
            }
        } catch (BufferOverflowException ex) {
            buf.reset();
            throw new RuntimeException("Buffer is full, can't serialize complete AppendEntryResponse to buf: " + buf, ex);
        }
    }
}
