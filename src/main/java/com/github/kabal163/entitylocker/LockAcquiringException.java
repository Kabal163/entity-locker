package com.github.kabal163.entitylocker;

public class LockAcquiringException extends RuntimeException {

    public LockAcquiringException(String message, Throwable cause) {
        super(message, cause);
    }
}
