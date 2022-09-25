package com.the123saurav.raftee.core.persistence;

import java.nio.ByteBuffer;

public interface Serializable {

    void serialize(ByteBuffer buf);
}
