package com.the123saurav.raftee.core.engine;

import com.the123saurav.raftee.api.BootException;
import com.the123saurav.raftee.api.RaftConfig;
import com.the123saurav.raftee.api.RaftEngine;
import com.the123saurav.raftee.core.persistence.BlockIO;
import com.the123saurav.raftee.core.persistence.FullIO;
import com.the123saurav.raftee.core.persistence.TermIO;
import com.the123saurav.raftee.core.persistence.VoteIO;
import com.the123saurav.raftee.core.persistence.model.IndexOffset;
import com.the123saurav.raftee.core.persistence.model.Log;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class SimpleRaftEngine implements RaftEngine, Runnable {
    private static final int READ_BUF_SIZE = 32 * 1024;
    private static final int WRITE_BUF_SIZE = 32 * 1024;

    private final Boot boot;
    private final ExecutorService engineExecutor;

    private long term;
    private String votedFor;

    private final FullIO<Long> termIO;
    private final FullIO<String> voteIO;

    private final BlockIO<IndexOffset> indexOffsetIO;
    private final BlockIO<Log> logIO;
    private final Map<Long, Long> termToIndex;


    public SimpleRaftEngine(RaftConfig config) throws IOException {
        log.info("In SimpleRaftEngine with config {}", config);
        engineExecutor = Executors.newVirtualThreadPerTaskExecutor();

        termIO = new TermIO(config.getDataDir());
        voteIO = new VoteIO(config.getDataDir());

        indexOffsetIO = BlockIO.<IndexOffset>of(Paths.get(config.getDataDir().toString(), "termindex"), READ_BUF_SIZE, WRITE_BUF_SIZE);
        logIO = BlockIO.<Log>of(Paths.get(config.getDataDir().toString(), "log"), READ_BUF_SIZE, WRITE_BUF_SIZE);
        termToIndex = new HashMap<>();
        boot = new Boot();
    }

    @Override
    public void run() {
        log.info("Starting raft engine...");
        boot.run();
        // Start TCP server listening on port

        // Start routine to process response from peers
        // Transition to follower state
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

    @Override
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(this, engineExecutor);
        // 1. read State

        // 2. Read (term, index) map

        // 3. Start all routines

        // 4. do while mismatch:
        //      - Send PrepareJoinCluster(term, index)

        // At this point, it knows leader and commitIndex.
        // Leader also knows the matchIndex.

        // 5. Replay logs now and apply to client FSM

        // 6. Send JoinCluster(matchIndex)
    }

    @Override
    public CompletableFuture<Void> stop() {
        return null;
    }
}
