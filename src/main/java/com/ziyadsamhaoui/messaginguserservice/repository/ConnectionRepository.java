package com.ziyadsamhaoui.messaginguserservice.repository;

import com.ziyadsamhaoui.messaginguserservice.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    Optional<Connection> findByConnectionHash(String connectionHash);

    boolean existsByConnectionHash(String connectionHash);

    Optional<Connection> findByUserId1AndUserId2(UUID userId1, UUID userId2);

    @Query("""
            select c from Connection c
            where (c.userId1 = :userId or c.userId2 = :userId)
              and c.status in ('PENDING', 'ACCEPTED')
            order by c.createdAt desc
            """)
    List<Connection> findActiveByParticipant(@Param("userId") UUID userId);

    @Query("""
            select c from Connection c
            where c.status = 'PENDING'
              and (c.userId1 = :userId or c.userId2 = :userId)
            order by c.createdAt desc
            """)
    List<Connection> findPendingInvolving(@Param("userId") UUID userId);
}
