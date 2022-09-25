package com.the123saurav.raftee.api;

public class BootException extends RuntimeException {
    public BootException(String msg, Throwable t) {
        super(msg, t);
    }
}
