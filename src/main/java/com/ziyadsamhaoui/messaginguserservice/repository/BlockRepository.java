package com.ziyadsamhaoui.messaginguserservice.repository;

import com.ziyadsamhaoui.messaginguserservice.model.Block;
import com.ziyadsamhaoui.messaginguserservice.model.BlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, BlockId> {

    @Query("""
            select count(b) > 0 from Block b
            where b.id.blockerId = :blockerId and b.id.blockedId = :blockedId
            """)
    boolean existsByBlockerIdAndBlockedId(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);

    @Query("""
            select b from Block b
            where b.id.blockerId = :blockerId
            """)
    List<Block> findByBlockerId(@Param("blockerId") UUID blockerId);

    @Modifying
    @Query("""
            delete from Block b
            where b.id.blockerId = :blockerId and b.id.blockedId = :blockedId
            """)
    long deleteByBlockerIdAndBlockedId(@Param("blockerId") UUID blockerId, @Param("blockedId") UUID blockedId);
}
