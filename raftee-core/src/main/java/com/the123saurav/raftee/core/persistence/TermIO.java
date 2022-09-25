package com.the123saurav.raftee.core.persistence;

import com.the123saurav.raftee.core.TransformUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public final class TermIO implements FullIO<Long> {
    private final RandomAccessFile file;
    private final byte[] buf;
    private boolean empty;

    public TermIO(Path dataDir) throws IOException {
        file = new RandomAccessFile(dataDir + "/term", "rwd");
        buf = new byte[8];
        if (file.length() == 0) {
            log.info("Creating termIo file.");
            file.write(TransformUtil.longToByteArr(0, buf));
            file.seek(0);
        }
    }

    @Override
    public Optional<Long> load() throws IOException {
        file.seek(0);
        if (file.length() == 0) {
            return Optional.empty();
        }
        if (file.read(buf) != buf.length) {
            throw new IllegalStateException("Cannot read term from file");
        }
        return Optional.of(TransformUtil.byteArrToLong(buf));
    }

    @Override
    public synchronized void write(Long data) throws IOException {
        empty = false;
        file.seek(0);
        TransformUtil.longToByteArr(data, buf);
        file.write(buf);
    }
}
