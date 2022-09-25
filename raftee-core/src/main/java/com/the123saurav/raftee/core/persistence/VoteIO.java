package com.the123saurav.raftee.core.persistence;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public final class VoteIO implements FullIO<String> {
    private final RandomAccessFile file;
    private final byte[] buf;

    public VoteIO(Path dataDir) throws FileNotFoundException {
        file = new RandomAccessFile(dataDir + "/vote", "rwd");
        buf = new byte[16];
    }

    @Override
    public Optional<String> load() throws IOException {
        file.seek(0);
        if (file.length() == 0) {
            return Optional.empty();
        }
        if (file.read(buf) != buf.length) {
            throw new IllegalStateException("Cannot read term from file");
        }
        return Optional.of(new String(buf, StandardCharsets.UTF_8));
    }

    @Override
    public void write(String data) throws IOException {
        file.seek(0);
        file.write(data.getBytes(StandardCharsets.UTF_8));
    }
}
