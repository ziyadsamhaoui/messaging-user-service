package com.ziyadsamhaoui.messaginguserservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ziyadsamhaoui.messaginguserservice.enums.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotNull UUID id,
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "must be alphanumeric with . _ - only")
            String username
    ) {}

    public record UpdateUserRequest(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "must be alphanumeric with . _ - only")
            String username,
            @Size(max = 512) String profilePictureUrl,
            @Size(max = 500) String description
    ) {}

    public record UpdateRoleRequest(
            @NotNull UserType type
    ) {}

    public record LastSeenRequest(
            @NotNull Instant seenAt
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PublicUserDto(
            UUID id,
            String username,
            String profilePictureUrl,
            String description,
            UserType type,
            Instant lastSeen,
            Instant createdAt
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AdminUserDto(
            UUID id,
            String username,
            UserType type,
            Instant lastSeen,
            Instant createdAt
    ) {}
}
