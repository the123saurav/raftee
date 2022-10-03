package com.the123saurav.raftee.core.network;

import com.the123saurav.raftee.core.RetryUtil;
import com.the123saurav.raftee.core.network.message.AppendEntryRequest;
import com.the123saurav.raftee.core.network.message.AppendEntryResponse;
import com.the123saurav.raftee.core.network.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;

import static com.the123saurav.raftee.core.network.message.MessageRegistry.APPEND_ENTRY_REQUEST;
import static com.the123saurav.raftee.core.network.message.MessageRegistry.APPEND_ENTRY_RESPONSE;

@Slf4j
public class Peer {
    private static final Set<Class> RETRYABLE_CONNECTION_EXCEPTIONS = new HashSet<>(Arrays.asList(IOException.class));
    private static final RetryUtil.RetryConfig CONNECTION_RETRY_CONFIG = new RetryUtil.RetryConfig("ensureConnected", 10, 2, 60000, -1,
            RETRYABLE_CONNECTION_EXCEPTIONS);

    private final ConnectionManager connectionManager;
    private final ClusterEndpoint remoteEp;
    private final ByteBuffer readBuf;
    private final ByteBuffer writeBuf;
    private SocketChannel channel;
    private ConcurrentLinkedQueue<Message> outgoingMsgQ;
    private ConcurrentLinkedQueue<Message> incomingMsgQ;

    public Peer(ConnectionManager connMgr, ClusterEndpoint ep, ConcurrentLinkedQueue<Message> reqQ,
                ConcurrentLinkedQueue<Message> respQ, ExecutorService executorService) throws IOException {
        connectionManager = connMgr;
        remoteEp = ep;
        channel = connectionManager.getBlockingConnection(ep);
        readBuf = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
        writeBuf = ByteBuffer.allocateDirect(channel.socket().getSendBufferSize());
        outgoingMsgQ = reqQ;
        incomingMsgQ = respQ;
        executorService.submit(this::handleWrite);
        executorService.submit(this::handleReceive);
    }

    /**
     * As per Raft paper, we need to retry infinite times if a peer is down.
     * We can get connected because we initiated or the peer connected to us.
     *
     * @return
     * @throws Exception
     */
    private synchronized boolean ensureConnected() throws Exception {
        return RetryUtil.retryWithBackoff(() -> {
            channel = connectionManager.getBlockingConnection(remoteEp);
            return true;
        }, CONNECTION_RETRY_CONFIG, log);
    }

    /**
     * Blocking call to join cluster
     *
     * @return
     * @throws IOException
     */
    public void handleWrite() {
        boolean needReconnect = false;
        while (!Thread.interrupted()) {
            try {
                if (needReconnect) {
                    ensureConnected();
                    needReconnect = false;
                }
                Message request = outgoingMsgQ.peek();
                if (request != null) {
                    log.debug("Sending req {} to ep {}", request, remoteEp);
                    try {
                        handleSend(request);
                        outgoingMsgQ.remove();
                    } catch (ClosedChannelException e) {
                        needReconnect = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                log.error(String.format("Got exception for peer %s in handleWrite", remoteEp), e);
            }
        }
        log.info("Shutting down write routine for peer {}", remoteEp);
    }

    /**
     * Blocking send of message
     *
     * @throws IOException
     */
    public void handleSend(Message request) throws IOException {
        writeBuf.clear();

        request.serialize(writeBuf);
        while (writeBuf.hasRemaining()) {
            channel.write(writeBuf);
        }
    }

    public void handleReceive() {
        while (!Thread.interrupted()) {
            try {
                if (channel.read(readBuf) == -1) {
                    log.error("Socket channel was closed by peer for ep {}", remoteEp);
                }
                readBuf.mark();
                try {
                    short msgSz = readBuf.getShort();
                    if (readBuf.remaining() < msgSz) {
                        readBuf.reset();
                        continue;
                    }
                    switch (readBuf.get()) {
                        case APPEND_ENTRY_REQUEST:
                            handleRequest(AppendEntryRequest::from, readBuf, (short) (msgSz - 1));
                        case APPEND_ENTRY_RESPONSE:
                            handleRequest(AppendEntryResponse::from, readBuf, (short) (msgSz - 1));

                    }
                } catch (final BufferOverflowException ex) {
                    readBuf.reset();
                }
            } catch (ClosedChannelException e) {
                try {
                    ensureConnected();
                } catch (Exception ex) {
                    throw new RuntimeException("Unable to connect with " + remoteEp, ex);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                readBuf.compact();
            }
        }
    }

    private void handleRequest(BiFunction<ByteBuffer, Short, Message> deserializer, ByteBuffer buffer, short msgSz) {
        Message request = deserializer.apply(buffer, msgSz);
        // Should never happen
        if (request == null) {
            throw new IllegalStateException("Request " +  request.getClass() + " is null unexpectedly");
        }
        incomingMsgQ.add(request);
    }
}
