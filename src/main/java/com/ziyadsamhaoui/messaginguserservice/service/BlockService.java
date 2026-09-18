package com.ziyadsamhaoui.messaginguserservice.service;

import com.ziyadsamhaoui.messaginguserservice.model.Block;
import com.ziyadsamhaoui.messaginguserservice.model.BlockId;
import com.ziyadsamhaoui.messaginguserservice.repository.BlockRepository;
import com.ziyadsamhaoui.messaginguserservice.exception.SelfBlockedException;
import com.ziyadsamhaoui.messaginguserservice.exception.UserNotFoundException;
import com.ziyadsamhaoui.messaginguserservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

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
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
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
