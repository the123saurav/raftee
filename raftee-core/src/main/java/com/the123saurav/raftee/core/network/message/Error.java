package com.the123saurav.raftee.core.network.message;

import com.the123saurav.raftee.core.persistence.Serializable;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @param code - status code sent by remote
 */
public record Error(short code, String message) implements Serializable {

    public static Error from(ByteBuffer buffer, short errSz) {
        buffer.mark();
        if (buffer.remaining() < errSz) {
            return null;
        }

        byte[] msg = new byte[errSz - 1];
        short code = buffer.get(); // Error code is 1 byte
        buffer.get(msg);
        return new Error(code, new String(msg, StandardCharsets.UTF_8));
    }

    @Override
    public void serialize(ByteBuffer buf) {
        buf.mark();
        try {
            buf.putShort((short) (2 + message.length()));
            buf.put((byte)code);
            buf.put(message.getBytes(StandardCharsets.UTF_8));
        } catch (BufferOverflowException ex) {
            buf.reset();
            throw new RuntimeException("Buffer is full, can't serialize complete Error to buf: " + buf, ex);
        }
    }

}
