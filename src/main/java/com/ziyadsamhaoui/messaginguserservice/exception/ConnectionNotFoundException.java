package com.ziyadsamhaoui.messaginguserservice.exception;

public class ConnectionNotFoundException extends RuntimeException {
    public ConnectionNotFoundException(Long id) {
        super("connection not found: " + id);
    }
}
