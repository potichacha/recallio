package com.recallio.group.infrastructure;

import com.recallio.group.domain.LeaderboardVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "group_settings")
public class GroupSettingsEntity {

    @Id
    @Column(name = "group_id")
    UUID groupId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "subject_slugs", nullable = false)
    String[] subjectSlugs = {};

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "theme_slugs", nullable = false)
    String[] themeSlugs = {};

    /** Les noms des valeurs de {@link com.recallio.content.CardType}. */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "card_types", nullable = false)
    String[] cardTypes = {};

    @Column(name = "daily_goal", nullable = false)
    int dailyGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "leaderboard_visibility", nullable = false)
    LeaderboardVisibility leaderboardVisibility = LeaderboardVisibility.NAMED;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt = Instant.EPOCH;

    protected GroupSettingsEntity() {}

    GroupSettingsEntity(UUID groupId) {
        this.groupId = groupId;
    }
}
