package com.ziyadsamhaoui.messaginguserservice.repository;

import com.ziyadsamhaoui.messaginguserservice.model.Block;
import com.ziyadsamhaoui.messaginguserservice.model.BlockId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, BlockId> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    List<Block> findByBlockerId(UUID blockerId);

    long deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
