package com.recallio.group.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_invitation")
public class GroupInvitationEntity {

    @Id
    @Column(name = "group_id")
    UUID groupId;

    @Column(nullable = false, unique = true)
    String token;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    protected GroupInvitationEntity() {}
}
