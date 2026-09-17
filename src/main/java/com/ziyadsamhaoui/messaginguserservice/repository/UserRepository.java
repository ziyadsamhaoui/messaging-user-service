package com.ziyadsamhaoui.messaginguserservice.repository;

import com.ziyadsamhaoui.messaginguserservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    @Query("""
            select u from User u
            where lower(u.username) like lower(concat(:q, '%'))
            order by u.username asc
            limit :limit
            """)
    List<User> searchByUsernamePrefix(@Param("q") String q, @Param("limit") int limit);

    @Modifying
    @Query("update User u set u.lastSeen = :seenAt where u.id = :id")
    int updateLastSeen(@Param("id") UUID id, @Param("seenAt") Instant seenAt);
}
