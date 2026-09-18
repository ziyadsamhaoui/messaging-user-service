package com.ziyadsamhaoui.messaginguserservice.exception;

public class SelfConnectionException extends RuntimeException {
    public SelfConnectionException() {
        super("users cannot connect to themselves");
    }
}
