package com.ziyadsamhaoui.messaginguserservice.service;

import com.ziyadsamhaoui.messaginguserservice.dto.ConnectionDto;
import com.ziyadsamhaoui.messaginguserservice.model.Connection;
import com.ziyadsamhaoui.messaginguserservice.enums.ConnectionStatus;
import com.ziyadsamhaoui.messaginguserservice.exception.*;
import com.ziyadsamhaoui.messaginguserservice.repository.ConnectionRepository;
import com.ziyadsamhaoui.messaginguserservice.common.ConnectionHasher;
import com.ziyadsamhaoui.messaginguserservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConnectionDto request(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new SelfConnectionException();
        }
        if (!userRepository.existsById(requesterId) || !userRepository.existsById(addresseeId)) {
            throw new UserNotFoundException(addresseeId);
        }
        String hash = ConnectionHasher.hash(requesterId, addresseeId);
        connectionRepository.findByConnectionHash(hash).ifPresent(existing -> {
            if (existing.getStatus() != ConnectionStatus.DECLINED) {
                throw new DuplicateConnectionException();
            }
            connectionRepository.delete(existing);
            connectionRepository.flush();
        });
        Connection connection = Connection.builder()
                .userId1(requesterId.compareTo(addresseeId) < 0 ? requesterId : addresseeId)
                .userId2(requesterId.compareTo(addresseeId) < 0 ? addresseeId : requesterId)
                .status(ConnectionStatus.PENDING)
                .connectionHash(hash)
                .build();
        try {
            return toDto(connectionRepository.saveAndFlush(connection), requesterId);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateConnectionException();
        }
    }

    @Transactional
    public ConnectionDto accept(UUID actingUserId, Long connectionId) {
        Connection connection = getConnectionOrThrow(connectionId);
        if (!connection.involves(actingUserId)) {
            throw new ForbiddenConnectionOperationException("not a participant of this connection");
        }
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new InvalidConnectionStateException("connection is not pending");
        }
        connection.accept();
        return toDto(connection, actingUserId);
    }

    @Transactional
    public ConnectionDto decline(UUID actingUserId, Long connectionId) {
        Connection connection = getConnectionOrThrow(connectionId);
        if (!connection.involves(actingUserId)) {
            throw new ForbiddenConnectionOperationException("not a participant of this connection");
        }
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new InvalidConnectionStateException("connection is not pending");
        }
        connection.decline();
        return toDto(connection, actingUserId);
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> listActive(UUID userId) {
        return connectionRepository.findActiveByParticipant(userId).stream()
                .map(connection -> toDto(connection, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConnectionDto> listPending(UUID userId) {
        return connectionRepository.findPendingInvolving(userId).stream()
                .map(connection -> toDto(connection, userId))
                .toList();
    }

    private Connection getConnectionOrThrow(Long connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ConnectionNotFoundException(connectionId));
    }

    private ConnectionDto toDto(Connection connection, UUID viewerId) {
        return new ConnectionDto(
                connection.getId(),
                connection.getStatus(),
                connection.otherParticipant(viewerId),
                connection.getCreatedAt()
        );
    }
}
