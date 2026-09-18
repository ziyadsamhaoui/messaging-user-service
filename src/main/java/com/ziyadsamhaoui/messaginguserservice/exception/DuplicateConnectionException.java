package com.ziyadsamhaoui.messaginguserservice.exception;

public class DuplicateConnectionException extends RuntimeException {
    public DuplicateConnectionException() {
        super("connection already exists between these users");
    }
}
