package com.ziyadsamhaoui.messaginguserservice.service;

import com.ziyadsamhaoui.messaginguserservice.model.User;
import com.ziyadsamhaoui.messaginguserservice.enums.UserType;
import com.ziyadsamhaoui.messaginguserservice.exception.DuplicateUsernameException;
import com.ziyadsamhaoui.messaginguserservice.exception.ForbiddenOperationException;
import com.ziyadsamhaoui.messaginguserservice.exception.RoleSyncException;
import com.ziyadsamhaoui.messaginguserservice.exception.UserNotFoundException;
import com.ziyadsamhaoui.messaginguserservice.outbox.TransactionalOutboxPublisher;
import com.ziyadsamhaoui.messaginguserservice.outbox.UserEvents;
import com.ziyadsamhaoui.messaginguserservice.repository.UserRepository;
import com.ziyadsamhaoui.messaginguserservice.security.AuthClient;
import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.CreateUserRequest;
import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.PublicUserDto;
import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.UpdateUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthClient authClient;
    private final TransactionalOutboxPublisher outboxPublisher;

    @Transactional
    public PublicUserDto createInternalUser(CreateUserRequest request) {
        if (userRepository.existsById(request.id())) {
            return userRepository.findById(request.id()).map(UserService::toPublicDto).orElseThrow();
        }
        User user = User.builder()
                .id(request.id())
                .username(request.username())
                .type(UserType.USER)
                .build();
        try {
            PublicUserDto dto = toPublicDto(userRepository.saveAndFlush(user));
            // Sprint 6 §2.2: same transaction as the insert, whatever the trigger —
            // the sync REST call or the CREDENTIAL_REGISTERED consumer (dual path).
            outboxPublisher.publish(TransactionalOutboxPublisher.AGGREGATE_TYPE, request.id().toString(),
                    UserEvents.USER_PROFILE_CREATED, new UserEvents.UserProfileCreated(
                            request.id(), request.username(), dto.createdAt()));
            return dto;
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUsernameException(request.username());
        }
    }

    @Transactional(readOnly = true)
    public PublicUserDto getPublicUser(UUID id) {
        return userRepository.findById(id).map(UserService::toPublicDto)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<PublicUserDto> search(String query, int limit) {
        String prefix = query.replaceAll("[%_]", "");
        if (prefix.isBlank()) {
            return List.of();
        }
        return userRepository.searchByUsernamePrefix(prefix, Math.min(limit, 50)).stream()
                .map(UserService::toPublicDto)
                .toList();
    }

    @Transactional
    public PublicUserDto updateProfile(UUID requesterId, UUID targetId, UpdateUserRequest request) {
        if (!requesterId.equals(targetId)) {
            throw new ForbiddenOperationException("users can only update their own profile");
        }
        userRepository.findByUsernameIgnoreCase(request.username())
                .filter(existing -> !existing.getId().equals(targetId))
                .ifPresent(existing -> {
                    throw new DuplicateUsernameException(request.username());
                });
        User user = getUserOrThrow(targetId);
        boolean usernameChanged = !user.getUsername().equalsIgnoreCase(request.username());
        user.applyProfileUpdate(request.username(), request.profilePictureUrl(), request.description());
        try {
            PublicUserDto dto = toPublicDto(userRepository.saveAndFlush(user));
            if (usernameChanged) {
                // Sprint 6 §2.2: User is authoritative for renames (flagged ambiguity in the
                // guide resolved against the actual PATCH /users/{id} handler, which accepts
                // username; Auth has no rename endpoint).
                outboxPublisher.publish(TransactionalOutboxPublisher.AGGREGATE_TYPE, targetId.toString(),
                        UserEvents.USER_USERNAME_CHANGED, new UserEvents.UserUsernameChanged(
                                targetId, request.username(), Instant.now()));
            }
            return dto;
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateUsernameException(request.username());
        }
    }

    @Transactional
    public PublicUserDto changeRole(UUID targetId, UserType newType) {
        User user = getUserOrThrow(targetId);
        UserType previous = user.getType();
        if (previous == newType) {
            return toPublicDto(user);
        }
        user.promoteTo(newType);
        try {
            authClient.changeRole(targetId, newType);
        } catch (RuntimeException ex) {
            user.promoteTo(previous);
            throw new RoleSyncException(targetId);
        }
        // Sprint 6 §2.2: additive to the synchronous PATCH /internal/credentials/{id}/role
        // call — that sync path (rollback-on-failure, 502) stays; this event is for later
        // Admin/audit consumers. Appended after the sync call succeeded so the rollback
        // path never leaves an orphan event behind.
        outboxPublisher.publish(TransactionalOutboxPublisher.AGGREGATE_TYPE, targetId.toString(),
                UserEvents.USER_ROLE_CHANGED, new UserEvents.UserRoleChanged(
                        targetId, newType.name(), Instant.now()));
        return toPublicDto(user);
    }

    @Transactional
    public void updateLastSeen(UUID userId, Instant seenAt) {
        if (userRepository.updateLastSeen(userId, seenAt) == 0) {
            throw new UserNotFoundException(userId);
        }
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    public static PublicUserDto toPublicDto(User user) {
        return new PublicUserDto(
                user.getId(),
                user.getUsername(),
                user.getProfilePictureUrl(),
                user.getDescription(),
                user.getType(),
                user.getLastSeen(),
                user.getCreatedAt()
        );
    }
}
