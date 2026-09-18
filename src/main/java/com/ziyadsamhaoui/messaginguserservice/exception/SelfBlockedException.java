package com.ziyadsamhaoui.messaginguserservice.exception;

public class SelfBlockedException extends RuntimeException {
    public SelfBlockedException() {
        super("users cannot block themselves");
    }
}
