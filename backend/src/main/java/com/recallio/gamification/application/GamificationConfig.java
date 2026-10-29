package com.recallio.gamification.application;

import com.recallio.gamification.domain.ScoringRules;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Barème et objectif quotidien ({@code recallio.gamification.*}). */
@ConfigMapping(prefix = "recallio.gamification")
public interface GamificationConfig {

    @WithDefault("10")
    int pointsPerReview();

    /** Cartes à réviser dans la journée (fuseau de l'utilisateur) pour qu'elle compte dans le streak. */
    @WithDefault("10")
    int dailyGoal();

    @WithDefault("50")
    int dailyGoalBonus();

    default ScoringRules rules() {
        return new ScoringRules(pointsPerReview(), dailyGoal(), dailyGoalBonus());
    }
}
