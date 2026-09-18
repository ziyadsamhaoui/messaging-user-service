package com.ziyadsamhaoui.messaginguserservice.model;

import com.ziyadsamhaoui.messaginguserservice.enums.ConnectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connections")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_1", nullable = false)
    private UUID userId1;

    @Column(name = "user_id_2", nullable = false)
    private UUID userId2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectionStatus status;

    @Column(name = "connection_hash", nullable = false, unique = true, length = 64)
    private String connectionHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean involves(UUID userId) {
        return userId1.equals(userId) || userId2.equals(userId);
    }

    public UUID otherParticipant(UUID userId) {
        return userId1.equals(userId) ? userId2 : userId1;
    }

    public void accept() {
        this.status = ConnectionStatus.ACCEPTED;
    }

    public void decline() {
        this.status = ConnectionStatus.DECLINED;
    }
}
