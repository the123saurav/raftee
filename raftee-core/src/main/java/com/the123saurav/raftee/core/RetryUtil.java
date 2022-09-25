package com.the123saurav.raftee.core;

import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.Callable;

public class RetryUtil {

    public record RetryConfig(String name, int initialBackoffMs, int multiplier, int maxBackoffMs, int numTries,
                              Set<Class> retryableExceptions) {
    }

    public static <T> T retryWithBackoff(Callable<T> callable, RetryConfig retryConfig, Logger log) throws Exception {
        long numTries = retryConfig.numTries;
        if (retryConfig.numTries == -1) {
            numTries = Long.MAX_VALUE;
        }
        Exception lastException = null;
        int retry = 0;
        int sleepTimeMs = retryConfig.initialBackoffMs;
        while (retry++ < numTries) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                log.error("Error in running callable " + retryConfig.name, e);
                if (!retryConfig.retryableExceptions.contains(e.getClass())) {
                    throw e;
                }
            }
            Thread.sleep(sleepTimeMs);
            sleepTimeMs *= retryConfig.multiplier;
            sleepTimeMs = Math.min(sleepTimeMs, retryConfig.maxBackoffMs);
        }
        throw new RuntimeException("Unable to finish callable", lastException);
    }
}
