package com.ziyadsamhaoui.messaginguserservice.dto;

import com.ziyadsamhaoui.messaginguserservice.enums.ConnectionStatus;

import java.time.Instant;
import java.util.UUID;

public record ConnectionDto(
        Long id,
        ConnectionStatus status,
        UUID otherUserId,
        Instant createdAt
) {}
