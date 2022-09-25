package com.the123saurav.raftee.core.persistence;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class BlockIO<T extends Serializable> {

    private final ByteBuffer readBuf;
    private final ByteBuffer writeBuf;
    private final FileChannel channel;
    private final Path filePath;

    private long endPos = 0;

    private BlockIO(Path path, FileChannel ch, ByteBuffer readB, ByteBuffer writeB) {
        filePath = path;
        channel = ch;
        readBuf = readB;
        writeBuf = writeB;
    }

    public static <T extends Serializable> BlockIO<T> of(Path filePath, int readBlockSize, int writeBlockSize) throws IOException {
        ByteBuffer readBuffer = ByteBuffer.allocate(readBlockSize);
        ByteBuffer writeBuffer = ByteBuffer.allocate(writeBlockSize);
        FileChannel fileChannel = FileChannel.open(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.SYNC);
        return new BlockIO<T>(filePath, fileChannel, readBuffer, writeBuffer);
    }

    /**
     * Synchronously appends a record to file.
     * @param record
     */
    public void append(T record) throws IOException {
        //channel.position(endPos);
        record.serialize(writeBuf);
        channel.write(writeBuf);
    }

    public List<T> readAll(Function<ByteBuffer, List<T>> deserializer) throws IOException {
        return readFrom(0, deserializer);
    }

    public List<T> readFrom(long offset, Function<ByteBuffer, List<T>> deserializer) throws IOException {
        log.info("Loading all entries from path {}", filePath);
        channel.position(offset);

        List<T> records = new ArrayList<>();
        int x;
        // TODO: check if this should be 0 too
        while ((x = channel.read(readBuf)) != -1) {
            readBuf.flip();
            log.info("x is {}", x);
            try {
                records.addAll(deserializer.apply(readBuf));
            } catch (BufferUnderflowException ex) {
                throw new RuntimeException("Unable to read full record from file " + filePath, ex);
            }
            readBuf.compact();
        }
        readBuf.flip();
        log.info("Read all from path {}, readbuf {}", filePath, readBuf);

        if (readBuf.hasRemaining()) {
            throw new RuntimeException("Unable to read all record from files " + filePath + " as it has " +
                    readBuf.remaining() + " bytes left");
        }
        log.info("Retrurning {} from path {}", records.size(), filePath);
        return records;
    }
}
