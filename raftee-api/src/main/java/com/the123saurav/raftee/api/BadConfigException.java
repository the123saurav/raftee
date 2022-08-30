package com.the123saurav.raftee.api;

public class BadConfigException extends RuntimeException {
    public BadConfigException(String message, Throwable t) {
        super(message, t);
    }
}
