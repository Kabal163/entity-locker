package com.github.kabal163;

public class ThreadIsNotOwnerException extends RuntimeException {

    public ThreadIsNotOwnerException(String message) {
        super(message);
    }
}
