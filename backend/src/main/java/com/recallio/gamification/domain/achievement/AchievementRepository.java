package com.recallio.gamification.domain.achievement;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Les succès débloqués par chaque utilisateur (ADR 0040). */
public interface AchievementRepository {

    /**
     * Débloque un succès, une seule fois : la première date reste.
     *
     * @return vrai si c'est cet appel qui l'a débloqué
     */
    boolean unlock(UUID userId, Achievement achievement, Instant unlockedAt);

    /** Les succès débloqués, avec leur date. */
    Map<Achievement, Instant> unlocked(UUID userId);
}
