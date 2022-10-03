package com.the123saurav.raftee.core.engine;

import com.the123saurav.raftee.core.network.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

public interface FSM {
    /**
     * Execute one iteration of the fsm state
     * @return next FSM state
     */
    FSM execute(LinkedBlockingQueue<Message> incomingMsgQ);
}
