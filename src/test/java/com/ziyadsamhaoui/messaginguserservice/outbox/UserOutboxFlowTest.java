package com.ziyadsamhaoui.messaginguserservice.outbox;

import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.CreateUserRequest;
import com.ziyadsamhaoui.messaginguserservice.dto.UserDtos.UpdateUserRequest;
import com.ziyadsamhaoui.messaginguserservice.enums.UserType;
import com.ziyadsamhaoui.messaginguserservice.model.Block;
import com.ziyadsamhaoui.messaginguserservice.model.BlockId;
import com.ziyadsamhaoui.messaginguserservice.model.Connection;
import com.ziyadsamhaoui.messaginguserservice.model.User;
import com.ziyadsamhaoui.messaginguserservice.repository.BlockRepository;
import com.ziyadsamhaoui.messaginguserservice.repository.ConnectionRepository;
import com.ziyadsamhaoui.messaginguserservice.repository.UserRepository;
import com.ziyadsamhaoui.messaginguserservice.security.AuthClient;
import com.ziyadsamhaoui.messaginguserservice.service.BlockService;
import com.ziyadsamhaoui.messaginguserservice.service.ConnectionService;
import com.ziyadsamhaoui.messaginguserservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserOutboxFlowTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthClient authClient;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private TransactionalOutboxPublisher outboxPublisher;

    private UserService userService;
    private BlockService blockService;
    private ConnectionService connectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        TransactionalOutboxPublisher realPublisher =
                new TransactionalOutboxPublisher(outboxEventRepository, objectMapper);
        userService = new UserService(userRepository, authClient, realPublisher);
        blockService = new BlockService(blockRepository, userRepository, realPublisher);
        connectionService = new ConnectionService(connectionRepository, userRepository, realPublisher);
    }

    @Test
    void profileCreationEmitsUserProfileCreated() {
        UUID id = UUID.randomUUID();
        User saved = User.builder().id(id).username("newuser").type(UserType.USER)
                .createdAt(Instant.now()).build();
        when(userRepository.existsById(id)).thenReturn(false);
        when(userRepository.saveAndFlush(any())).thenReturn(saved);

        userService.createInternalUser(new CreateUserRequest(id, "newuser"));

        ArgumentCaptor<OutboxEvent> row = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(row.capture());
        OutboxEvent event = row.getValue();
        assertThat(event.getEventType()).isEqualTo(UserEvents.USER_PROFILE_CREATED);
        assertThat(event.getAggregateId()).isEqualTo(id.toString());
        assertThat(event.getAggregateType()).isEqualTo("UserProfile");
        UserEvents.UserProfileCreated payload = objectMapper.readValue(
                event.getPayload(), UserEvents.UserProfileCreated.class);
        assertThat(payload.userId()).isEqualTo(id);
        assertThat(payload.username()).isEqualTo("newuser");
    }

    @Test
    void replayedProfileCreationIsIdempotentAndEmitsNoSecondEvent() {
        UUID id = UUID.randomUUID();
        User existing = User.builder().id(id).username("existing").type(UserType.USER)
                .createdAt(Instant.now()).build();
        when(userRepository.existsById(id)).thenReturn(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        userService.createInternalUser(new CreateUserRequest(id, "existing"));

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void usernameRenameEmitsUserUsernameChanged() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("oldname").type(UserType.USER)
                .createdAt(Instant.now()).build();
        when(userRepository.findByUsernameIgnoreCase("newname")).thenReturn(Optional.empty());
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenReturn(user);

        userService.updateProfile(id, id, new UpdateUserRequest("newname", null, null));

        ArgumentCaptor<OutboxEvent> row = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(row.capture());
        assertThat(row.getValue().getEventType()).isEqualTo(UserEvents.USER_USERNAME_CHANGED);
        UserEvents.UserUsernameChanged payload = objectMapper.readValue(
                row.getValue().getPayload(), UserEvents.UserUsernameChanged.class);
        assertThat(payload.newUsername()).isEqualTo("newname");
    }

    @Test
    void profileUpdateWithoutRenameEmitsNoUsernameEvent() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("same").type(UserType.USER)
                .createdAt(Instant.now()).build();
        when(userRepository.findByUsernameIgnoreCase("same")).thenReturn(Optional.of(user));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any())).thenReturn(user);

        userService.updateProfile(id, id, new UpdateUserRequest("same", "http://pic", "hello"));

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void roleChangeEmitsUserRoleChangedAfterSyncCallSucceeds() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("someone").type(UserType.USER)
                .createdAt(Instant.now()).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.changeRole(id, UserType.ADMIN);

        ArgumentCaptor<OutboxEvent> row = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(row.capture());
        assertThat(row.getValue().getEventType()).isEqualTo(UserEvents.USER_ROLE_CHANGED);
        verify(authClient).changeRole(id, UserType.ADMIN);
    }

    @Test
    void roleSyncFailureEmitsNoEvent() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).username("someone").type(UserType.USER)
                .createdAt(Instant.now()).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        org.mockito.Mockito.doThrow(new RuntimeException("auth down"))
                .when(authClient).changeRole(id, UserType.ADMIN);

        try {
            userService.changeRole(id, UserType.ADMIN);
        } catch (RuntimeException expected) {
            // RoleSyncException path
        }

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void blockEmitsUserBlocked() {
        UUID blocker = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        when(userRepository.existsById(blocked)).thenReturn(true);
        when(blockRepository.existsByBlockerIdAndBlockedId(blocker, blocked)).thenReturn(false);

        blockService.block(blocker, blocked);

        ArgumentCaptor<OutboxEvent> row = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(row.capture());
        assertThat(row.getValue().getEventType()).isEqualTo(UserEvents.USER_BLOCKED);
        UserEvents.UserBlocked payload = objectMapper.readValue(
                row.getValue().getPayload(), UserEvents.UserBlocked.class);
        assertThat(payload.blockerId()).isEqualTo(blocker);
        assertThat(payload.blockedId()).isEqualTo(blocked);
    }

    @Test
    void duplicateBlockEmitsNoEvent() {
        UUID blocker = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        when(userRepository.existsById(blocked)).thenReturn(true);
        when(blockRepository.existsByBlockerIdAndBlockedId(blocker, blocked)).thenReturn(true);

        blockService.block(blocker, blocked);

        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void unblockEmitsUserUnblockedOnlyWhenRowExisted() {
        UUID blocker = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        when(blockRepository.deleteByBlockerIdAndBlockedId(blocker, blocked)).thenReturn(1L);

        blockService.unblock(blocker, blocked);

        ArgumentCaptor<OutboxEvent> row = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(row.capture());
        assertThat(row.getValue().getEventType()).isEqualTo(UserEvents.USER_UNBLOCKED);

        org.mockito.Mockito.clearInvocations(outboxEventRepository);
        when(blockRepository.deleteByBlockerIdAndBlockedId(blocker, blocked)).thenReturn(0L);
        blockService.unblock(blocker, blocked);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void acceptedConnectionEmitsUserConnectionAccepted() {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID lowId = alice.compareTo(bob) < 0 ? alice : bob;
        UUID highId = alice.compareTo(bob) < 0 ? bob : alice;
        Connection connection = Connection.builder()
                .id(1L)
                .userId1(lowId)
                .userId2(highId)
                .status(com.ziyadsamhaoui.messaginguserservice.enums.ConnectionStatus.PENDING)
                .connectionHash("hash")
                .build();
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));
        ReflectionTestUtils.setField(connection, "status",
                com.ziyadsamhaoui.messaginguserservice.enums.ConnectionStatus.PENDING);

        connectionService.accept(bob, 1L);

        ArgumentCaptor<OutboxEvent> row = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(row.capture());
        assertThat(row.getValue().getEventType()).isEqualTo(UserEvents.USER_CONNECTION_ACCEPTED);
        UserEvents.UserConnectionAccepted payload = objectMapper.readValue(
                row.getValue().getPayload(), UserEvents.UserConnectionAccepted.class);
        assertThat(payload.userIdA()).isEqualTo(lowId);
        assertThat(payload.userIdB()).isEqualTo(highId);
    }
}
