package com.recallio.gamification.application;

import com.recallio.gamification.domain.achievement.Achievement;
import com.recallio.gamification.domain.achievement.AchievementRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Cas d'usage : tous les succès, débloqués ou non, d'un utilisateur (ADR 0040). */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class AchievementService {

    private final AchievementRepository achievements;

    public AchievementService(AchievementRepository achievements) {
        this.achievements = achievements;
    }

    /** Dans l'ordre du catalogue. */
    public List<AchievementView> all(UserRef user) {
        Map<Achievement, Instant> unlocked = achievements.unlocked(user.id());
        return Arrays.stream(Achievement.values())
                .map(achievement -> new AchievementView(achievement, unlocked.get(achievement)))
                .toList();
    }

    /** @param unlockedAt absent tant que le succès n'est pas débloqué */
    public record AchievementView(
            Achievement achievement, @Nullable Instant unlockedAt) {}
}
