package com.ziyadsamhaoui.messaginguserservice.model;

import com.ziyadsamhaoui.messaginguserservice.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "profile_picture_url", length = 512)
    private String profilePictureUrl;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserType type;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Immutable
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void applyProfileUpdate(String username, String profilePictureUrl, String description) {
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
        this.description = description;
    }

    public void markSeen(Instant seenAt) {
        this.lastSeen = seenAt;
    }

    public void promoteTo(UserType type) {
        this.type = type;
    }
}
