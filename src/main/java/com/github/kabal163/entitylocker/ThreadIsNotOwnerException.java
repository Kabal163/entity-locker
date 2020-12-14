package com.github.kabal163.entitylocker;

public class ThreadIsNotOwnerException extends RuntimeException {

    public ThreadIsNotOwnerException(String message) {
        super(message);
    }
}
