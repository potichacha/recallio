package com.recallio.gamification.infrastructure;

import com.recallio.gamification.domain.achievement.Achievement;
import com.recallio.gamification.domain.achievement.AchievementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JpaAchievementRepository implements AchievementRepository {

    private final EntityManager entityManager;

    public JpaAchievementRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** {@code ON CONFLICT DO NOTHING} : la date du premier déblocage reste. */
    @Override
    public boolean unlock(UUID userId, Achievement achievement, Instant unlockedAt) {
        return entityManager
                        .createNativeQuery("""
                                insert into achievement (user_id, code, unlocked_at) values (:userId, :code, :at)
                                on conflict (user_id, code) do nothing
                                """)
                        .setParameter("userId", userId)
                        .setParameter("code", achievement.name())
                        .setParameter("at", unlockedAt)
                        .executeUpdate()
                > 0;
    }

    @Override
    public Map<Achievement, Instant> unlocked(UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery("select code, unlocked_at from achievement where user_id = :userId")
                .setParameter("userId", userId)
                .getResultList();
        Map<Achievement, Instant> unlocked = new EnumMap<>(Achievement.class);
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            unlocked.put(Achievement.valueOf((String) columns[0]), instant(columns[1]));
        }
        return unlocked;
    }

    private static Instant instant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case OffsetDateTime dateTime -> dateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("date inattendue : " + value.getClass());
        };
    }
}
