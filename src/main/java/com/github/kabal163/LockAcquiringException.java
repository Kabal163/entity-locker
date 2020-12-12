package com.github.kabal163;

public class LockAcquiringException extends RuntimeException {

    public LockAcquiringException(String message, Throwable cause) {
        super(message, cause);
    }
}
