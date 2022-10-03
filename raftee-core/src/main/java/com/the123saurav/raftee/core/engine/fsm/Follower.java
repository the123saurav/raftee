package com.the123saurav.raftee.core.engine.fsm;

import com.the123saurav.raftee.core.engine.FSM;
import com.the123saurav.raftee.core.network.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

public class Follower implements FSM {

    /**
     * 1. Start timer, checking in between for heartbeats
     *  1.1. If receiver HB from leader, move to joinCluster if needed and then transition to Follower
     *  1.2. Move to Candidate
     * @return
     */
    @Override
    public FSM execute(LinkedBlockingQueue<Message> incomingMsgQ) {
        return this;
    }
}
