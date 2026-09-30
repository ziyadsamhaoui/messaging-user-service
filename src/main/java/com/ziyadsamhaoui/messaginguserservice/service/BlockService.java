package com.ziyadsamhaoui.messaginguserservice.service;

import com.ziyadsamhaoui.messaginguserservice.model.Block;
import com.ziyadsamhaoui.messaginguserservice.model.BlockId;
import com.ziyadsamhaoui.messaginguserservice.outbox.TransactionalOutboxPublisher;
import com.ziyadsamhaoui.messaginguserservice.outbox.UserEvents;
import com.ziyadsamhaoui.messaginguserservice.repository.BlockRepository;
import com.ziyadsamhaoui.messaginguserservice.exception.SelfBlockedException;
import com.ziyadsamhaoui.messaginguserservice.exception.UserNotFoundException;
import com.ziyadsamhaoui.messaginguserservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final TransactionalOutboxPublisher outboxPublisher;

    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new SelfBlockedException();
        }
        if (!userRepository.existsById(blockedId)) {
            throw new UserNotFoundException(blockedId);
        }
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return;
        }
        blockRepository.save(new Block(new BlockId(blockerId, blockedId), null));
        // Sprint 6 §2.2: same transaction as the block insert; consumed by Chat's block cache.
        outboxPublisher.publish(TransactionalOutboxPublisher.AGGREGATE_TYPE, blockerId.toString(),
                UserEvents.USER_BLOCKED, new UserEvents.UserBlocked(blockerId, blockedId, Instant.now()));
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        long deleted = blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        // Only emit when a row actually went away: unblocking a non-existent block
        // must not produce an event.
        if (deleted > 0) {
            outboxPublisher.publish(TransactionalOutboxPublisher.AGGREGATE_TYPE, blockerId.toString(),
                    UserEvents.USER_UNBLOCKED, new UserEvents.UserUnblocked(blockerId, blockedId));
        }
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(UUID aId, UUID bId) {
        return blockRepository.existsByBlockerIdAndBlockedId(aId, bId)
                || blockRepository.existsByBlockerIdAndBlockedId(bId, aId);
    }

    @Transactional(readOnly = true)
    public List<UUID> listBlockedUsers(UUID blockerId) {
        return blockRepository.findByBlockerId(blockerId).stream()
                .map(block -> block.getId().getBlockedId())
                .toList();
    }
}
