package com.recallio.group.infrastructure;

import com.recallio.content.CardType;
import com.recallio.group.domain.GroupSettings;
import com.recallio.group.domain.GroupSettingsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaGroupSettingsRepository implements GroupSettingsRepository {

    private final EntityManager entityManager;

    public JpaGroupSettingsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public GroupSettings findByGroup(UUID groupId) {
        GroupSettingsEntity entity = entityManager.find(GroupSettingsEntity.class, groupId);
        if (entity == null) {
            return GroupSettings.DEFAULTS;
        }
        return new GroupSettings(
                List.of(entity.subjectSlugs),
                List.of(entity.themeSlugs),
                Arrays.stream(entity.cardTypes).map(CardType::valueOf).collect(Collectors.toSet()),
                entity.dailyGoal,
                entity.leaderboardVisibility);
    }

    @Override
    public void save(UUID groupId, GroupSettings settings, Instant updatedAt) {
        GroupSettingsEntity existing = entityManager.find(GroupSettingsEntity.class, groupId);
        GroupSettingsEntity entity = existing == null ? new GroupSettingsEntity(groupId) : existing;
        entity.subjectSlugs = settings.subjectSlugs().toArray(String[]::new);
        entity.themeSlugs = settings.themeSlugs().toArray(String[]::new);
        // Triés : la ligne ne change pas d'un enregistrement à l'autre si le choix est le même.
        entity.cardTypes =
                settings.cardTypes().stream().map(CardType::name).sorted().toArray(String[]::new);
        entity.dailyGoal = settings.dailyGoal();
        entity.leaderboardVisibility = settings.leaderboardVisibility();
        entity.updatedAt = updatedAt;
        // Persistée une fois remplie : une requête qui suivrait (flush) l'insérerait sinon avec ses valeurs initiales.
        if (existing == null) {
            entityManager.persist(entity);
        }
    }
}
