package com.ziyadsamhaoui.messaginguserservice.exception;

import java.util.UUID;

public class RoleSyncException extends RuntimeException {
    public RoleSyncException(UUID userId) {
        super("failed to sync role with auth service for user: " + userId);
    }
}
