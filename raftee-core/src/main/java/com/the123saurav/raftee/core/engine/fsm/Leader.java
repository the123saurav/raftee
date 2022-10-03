package com.the123saurav.raftee.core.engine.fsm;

import com.the123saurav.raftee.core.engine.FSM;
import com.the123saurav.raftee.core.network.message.Message;

import java.util.concurrent.LinkedBlockingQueue;

public class Leader implements FSM {
    @Override
    public FSM execute(LinkedBlockingQueue<Message> incomingMsgQ) {
        return null;
    }
}
