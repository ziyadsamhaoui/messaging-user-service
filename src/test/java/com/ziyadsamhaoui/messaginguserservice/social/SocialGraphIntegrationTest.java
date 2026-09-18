package com.ziyadsamhaoui.messaginguserservice.social;

import com.ziyadsamhaoui.messaginguserservice.TestcontainersConfiguration;
import com.ziyadsamhaoui.messaginguserservice.repository.BlockRepository;
import com.ziyadsamhaoui.messaginguserservice.service.BlockService;
import com.ziyadsamhaoui.messaginguserservice.exception.SelfBlockedException;
import com.ziyadsamhaoui.messaginguserservice.model.Connection;
import com.ziyadsamhaoui.messaginguserservice.common.ConnectionHasher;
import com.ziyadsamhaoui.messaginguserservice.repository.ConnectionRepository;
import com.ziyadsamhaoui.messaginguserservice.service.ConnectionService;
import com.ziyadsamhaoui.messaginguserservice.enums.ConnectionStatus;
import com.ziyadsamhaoui.messaginguserservice.exception.DuplicateConnectionException;
import com.ziyadsamhaoui.messaginguserservice.model.User;
import com.ziyadsamhaoui.messaginguserservice.repository.UserRepository;
import com.ziyadsamhaoui.messaginguserservice.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SocialGraphIntegrationTest {

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private BlockService blockService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        blockRepository.deleteAll();
        connectionRepository.deleteAll();
        userRepository.deleteAll();
    }

    private UUID seedUser(String username) {
        UUID id = UUID.randomUUID();
        userRepository.save(User.builder().id(id).username(username).type(UserType.USER).build());
        return id;
    }

    @Test
    void blockRejectsSelfBlocking() {
        UUID id = seedUser("blocker");
        assertThatThrownBy(() -> blockService.block(id, id))
                .isInstanceOf(SelfBlockedException.class);
    }

    @Test
    void blockAndCheckIsSymmetric() {
        UUID a = seedUser("alice");
        UUID b = seedUser("bella");
        blockService.block(a, b);
        assertThat(blockService.isBlocked(a, b)).isTrue();
        assertThat(blockService.isBlocked(b, a)).isTrue();
    }

    @Test
    void unblockRemovesBlock() {
        UUID a = seedUser("carol");
        UUID b = seedUser("dora");
        blockService.block(a, b);
        blockService.unblock(a, b);
        assertThat(blockService.isBlocked(a, b)).isFalse();
    }

    @Test
    void connectionHashIsDeterministicAndOrderIndependent() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThat(ConnectionHasher.hash(a, b)).isEqualTo(ConnectionHasher.hash(b, a));
        assertThat(ConnectionHasher.hash(a, b)).hasSize(64);
    }

    @Test
    void duplicateConnectionPairsRejectedRegardlessOfArgumentOrder() {
        UUID a = seedUser("erin");
        UUID b = seedUser("frank");
        connectionService.request(a, b);
        assertThatThrownBy(() -> connectionService.request(b, a))
                .isInstanceOf(DuplicateConnectionException.class);
        assertThat(connectionRepository.count()).isEqualTo(1);
    }

    @Test
    void acceptFlowMovesPendingToAccepted() {
        UUID a = seedUser("grace");
        UUID b = seedUser("heidi");
        var dto = connectionService.request(a, b);
        var accepted = connectionService.accept(b, dto.id());
        assertThat(accepted.status()).isEqualTo(ConnectionStatus.ACCEPTED);
    }

    @Test
    void onlyAddresseeFlowParticipantCanAccept() {
        UUID a = seedUser("isabel");
        UUID b = seedUser("jack");
        UUID outsider = seedUser("otto");
        var dto = connectionService.request(a, b);
        Long connectionId = dto.id();
        assertThatThrownBy(() -> connectionService.accept(outsider, connectionId))
                .isInstanceOf(Exception.class);
        assertThat(connectionService.accept(b, connectionId).status())
                .isEqualTo(ConnectionStatus.ACCEPTED);
    }

    @Test
    void declinedRequestCanBeReRequested() {
        UUID a = seedUser("kate");
        UUID b = seedUser("liam");
        var first = connectionService.request(a, b);
        connectionService.decline(b, first.id());
        var second = connectionService.request(a, b);
        assertThat(second.status()).isEqualTo(ConnectionStatus.PENDING);
        assertThat(connectionRepository.count()).isEqualTo(1);
    }

    @Test
    void directEntityInsertWithSamePairViolatesUniqueHashConstraint() {
        UUID a = seedUser("mallory");
        UUID b = seedUser("nathan");
        String hash = ConnectionHasher.hash(a, b);
        connectionRepository.save(Connection.builder()
                .userId1(a).userId2(b)
                .status(ConnectionStatus.PENDING)
                .connectionHash(hash)
                .build());
        connectionRepository.flush();
        assertThatThrownBy(() -> {
            connectionRepository.save(Connection.builder()
                    .userId1(b).userId2(a)
                    .status(ConnectionStatus.PENDING)
                    .connectionHash(hash)
                    .build());
            connectionRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
