package com.recallio.gamification.application;

import com.recallio.gamification.domain.XpRules;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/** Barème de l'XP et objectif quotidien ({@code recallio.gamification.*}, ADR 0035). */
@ConfigMapping(prefix = "recallio.gamification")
public interface GamificationConfig {

    /** L'unité du barème : l'XP d'une question de cours. Les autres formats en sont des multiples. */
    @WithDefault("10")
    int xpPerReview();

    /** Cartes à réviser dans la journée (fuseau de l'utilisateur) pour qu'elle compte dans le streak. */
    @WithDefault("10")
    int dailyGoal();

    @WithDefault("50")
    int dailyGoalBonus();

    default XpRules rules() {
        return new XpRules(xpPerReview(), dailyGoal(), dailyGoalBonus());
    }
}
