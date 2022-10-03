package com.the123saurav.raftee.core.engine;

import com.the123saurav.raftee.api.BootException;
import com.the123saurav.raftee.api.RaftConfig;
import com.the123saurav.raftee.api.RaftEngine;
import com.the123saurav.raftee.core.engine.fsm.Follower;
import com.the123saurav.raftee.core.network.message.Message;
import com.the123saurav.raftee.core.persistence.BlockIO;
import com.the123saurav.raftee.core.persistence.FullIO;
import com.the123saurav.raftee.core.persistence.TermIO;
import com.the123saurav.raftee.core.persistence.VoteIO;
import com.the123saurav.raftee.core.persistence.model.IndexOffset;
import com.the123saurav.raftee.core.persistence.model.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class SimpleRaftEngine implements RaftEngine, Runnable {
    private static final int READ_BUF_SIZE = 32 * 1024;
    private static final int WRITE_BUF_SIZE = 32 * 1024;

    private static final int SHUTDOWN_TIMEOUT_SEC = 30;

    private final Boot boot;
    private final int listenPort;
    private final ExecutorService engineExecutor;

    private long term;
    private String votedFor;

    private final FullIO<Long> termIO;
    private final FullIO<String> voteIO;

    private final BlockIO<IndexOffset> indexOffsetIO;
    private final BlockIO<Log> logIO;
    private final Map<Long, Long> termToIndex;
    private ResponseHandler responseHandler;

    private EngineState engineState;
    /*
     TODO: below can an ArrayBlockingQueue, if need be, to prevent node allocation
     every time and also have backpressure on TCP socket in Peer.java
     */
    private LinkedBlockingQueue<Message> incomingMsgQ;

    private enum EngineState {
        CREATED,
        RUNNING,
        TERMINATING,
        TERMINATED
    }


    public SimpleRaftEngine(RaftConfig config, int port) throws IOException {
        log.info("In SimpleRaftEngine with config {}", config);
        engineExecutor = Executors.newVirtualThreadPerTaskExecutor();

        termIO = new TermIO(config.getDataDir());
        voteIO = new VoteIO(config.getDataDir());

        indexOffsetIO = BlockIO.<IndexOffset>of(Paths.get(config.getDataDir().toString(), "termindex"), READ_BUF_SIZE, WRITE_BUF_SIZE);
        logIO = BlockIO.<Log>of(Paths.get(config.getDataDir().toString(), "log"), READ_BUF_SIZE, WRITE_BUF_SIZE);
        termToIndex = new HashMap<>();
        boot = new Boot();
        listenPort = port;
        engineState = EngineState.CREATED;
    }

    @Override
    public void run() {
        log.info("Starting raft engine...");
        boot.run();
        // Start TCP server listening on port
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(listenPort));
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        } catch (IOException e) {
            throw new RuntimeException("Unable to start TCP server", e);
        }

        log.info("Listening on port {}", listenPort);
        // Start routine to process response from peers
        incomingMsgQ = new LinkedBlockingQueue<>();
        responseHandler = new ResponseHandler();
        engineExecutor.submit(responseHandler);
        // Transition to follower state
        engineExecutor.submit(() -> {
            FSM fsm = new Follower();
            while (!Thread.interrupted()) {
                fsm = fsm.execute(incomingMsgQ);
            }
            log.info("Shutting down RaftEngine");
        });
        // engine started at this point
    }

    private class Boot implements Runnable {

        @Override
        public void run() {
            log.info("Started booting Raft engine...");
            try {
                term = termIO.load().orElse(0L) + 1;
                log.info("Loaded termIo.");
                votedFor = voteIO.load().orElse(null);
                log.info("Loaded voteIO.");
                // read and prepare temp map from IndexOffset and log
                // Index in log file is 1 based
                AtomicLong maxOffset = new AtomicLong(0);
                AtomicLong maxTerm = new AtomicLong(0);
                AtomicLong maxIndex = new AtomicLong(0);

                indexOffsetIO.readAll(IndexOffset::from)
                        .forEach(i -> {
                            termToIndex.put(i.term(), i.index());
                            if (maxIndex.get() < i.index()) { // Isnt this always true?
                                maxIndex.set(i.index());
                                maxTerm.set(i.term());
                                maxOffset.set(i.offset());
                            }
                        });
                log.info("Loaded indexOffsetIO.");

                List<Log> logs = logIO.readFrom(maxOffset.get(), Log::from);
                if (logs.size() > 0) {
                    Log currLog = null;
                    for (int in = 0; in < logs.size(); ++in) {
                        currLog = logs.get(in);
                        if (currLog.term() > maxTerm.get()) {
                            if (in > 0) {
                                termToIndex.put(maxTerm.get(), logs.get(in - 1).index());
                            }
                            maxTerm.set(currLog.term());
                        }
                    }
                    termToIndex.put(maxTerm.get(), currLog.index());
                }
                // At this point we have constructed the termToIndex map.
            } catch (IOException e) {
                throw new BootException("Error in booting raft engine", e);
            }
            log.info("Successfully booted Raft engine");
        }
    }

    @RequiredArgsConstructor
    private class ResponseHandler implements Runnable {

        @Override
        public void run() {
            log.info("Started ResponseHandler");
            Message msg;
            while (!Thread.interrupted()) {
                try {
                    msg = incomingMsgQ.take();
                } catch (InterruptedException e) {
                    log.info("Interrupted, shutting down thread");
                    return;
                }
                // handle msg
            }
        }
    }
    @Override
    public synchronized CompletableFuture<Void> start() {
        if (engineState.equals(EngineState.RUNNING) || engineState.equals(EngineState.TERMINATING)) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    String.format("engine in state %s can't be started again", engineState)));
        }
        return CompletableFuture.runAsync(this, engineExecutor);
    }

    @Override
    public synchronized CompletableFuture<Void> stop() {
        if (engineState.equals(EngineState.CREATED)) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    String.format("engine in state %s can't be started again", engineState)));
        }
        // Run in default executor
        return CompletableFuture.runAsync(() -> {
            // Idempotent
            engineExecutor.shutdownNow();
            try {
                engineExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while awaiting termination", e);
            }
        });
    }
}
