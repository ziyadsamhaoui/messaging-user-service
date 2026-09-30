package com.ziyadsamhaoui.messaginguserservice.outbox;

import java.time.Instant;
import java.util.UUID;


public final class UserEvents {

    private UserEvents() {
    }

    public static final String USER_PROFILE_CREATED = "USER_PROFILE_CREATED";
    public static final String USER_USERNAME_CHANGED = "USER_USERNAME_CHANGED";
    public static final String USER_ROLE_CHANGED = "USER_ROLE_CHANGED";
    public static final String USER_BLOCKED = "USER_BLOCKED";
    public static final String USER_UNBLOCKED = "USER_UNBLOCKED";
    public static final String USER_CONNECTION_ACCEPTED = "USER_CONNECTION_ACCEPTED";

    public record UserProfileCreated(UUID userId, String username, Instant createdAt) {
    }

    public record UserUsernameChanged(UUID userId, String newUsername, Instant changedAt) {
    }

    public record UserRoleChanged(UUID userId, String role, Instant changedAt) {
    }

    public record UserBlocked(UUID blockerId, UUID blockedId, Instant blockedAt) {
    }

    public record UserUnblocked(UUID blockerId, UUID blockedId) {
    }

    public record UserConnectionAccepted(UUID userIdA, UUID userIdB, Instant acceptedAt) {
    }
}
