package com.recallio.group.infrastructure;

import com.recallio.group.domain.GroupRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_member")
@IdClass(GroupMemberEntity.Key.class)
public class GroupMemberEntity {

    @Id
    @Column(name = "group_id")
    UUID groupId;

    @Id
    @Column(name = "user_id")
    UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    GroupRole role;

    @Column(name = "joined_at", nullable = false)
    Instant joinedAt;

    protected GroupMemberEntity() {}

    GroupMemberEntity(UUID groupId, UUID userId, GroupRole role, Instant joinedAt) {
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    /** Clé composite (groupe, utilisateur). */
    public record Key(UUID groupId, UUID userId) implements Serializable {}
}
