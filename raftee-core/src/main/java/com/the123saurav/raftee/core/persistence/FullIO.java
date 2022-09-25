package com.the123saurav.raftee.core.persistence;

import java.io.IOException;
import java.util.Optional;

public interface FullIO<T> {
    Optional<T> load() throws IOException;
    void write(T data) throws IOException;
}
