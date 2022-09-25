package com.the123saurav.raftee.core.network.message;


import com.the123saurav.raftee.core.persistence.model.Log;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public record AppendEntryRequest(UUID id, FollowerState followerState, long term, long lastIndexForTerm, List<Log> logs) implements Message {

    public static AppendEntryRequest from(ByteBuffer buffer, short msgSz) {
        buffer.mark();
        try {
            byte[] uuid = new byte[36];
            buffer.get(uuid);
            // Do we need to optimize on this? have a map?
            UUID id = UUID.fromString(new String(uuid, StandardCharsets.UTF_8));
            FollowerState followerState = FollowerState.from(buffer.get());
            long term = buffer.getLong();
            long index = buffer.getLong();

            short numLogs = buffer.getShort();
            List<Log> logs = Log.from(buffer, numLogs);
            /*Log[] logs = new Log[numLogs];

            int logSz;
            long logT, logIn;
            for(int in=0; in < numLogs; ++in) {
                logT = buffer.getLong();
                logIn = buffer.getLong();
                logSz = buffer.getInt();
                byte[] logD = new byte[logSz];
                buffer.get(logD);
                logs[in] = new Log()
            }*/
            return new AppendEntryRequest(id, followerState, term, index, logs);
        } catch (BufferUnderflowException ex) {
            buffer.reset();
            return null;
        }
    }

    @Override
    public void serialize(ByteBuffer buf) {
        buf.mark();
        try {
            String idStr = id.toString();

            buf.putShort((short) (idStr.length() + 1 + 8 + 8));

            buf.put(MessageRegistry.APPEND_ENTRY_REQUEST);

            buf.putShort((short) idStr.length());
            buf.put(idStr.getBytes(StandardCharsets.UTF_8));
            buf.put(followerState.serialize());
            buf.putLong(term);
            buf.putLong(lastIndexForTerm);

            for (Log l : logs) {
                l.serialize(buf);
            }
        } catch (BufferOverflowException ex) {
            buf.reset();
            throw new RuntimeException("Buffer is full, can't serialize complete AppendEntryRequest to buf: " + buf, ex);
        }
    }
}
