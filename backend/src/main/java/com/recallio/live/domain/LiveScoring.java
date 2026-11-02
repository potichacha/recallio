package com.recallio.live.domain;

import java.time.Duration;

/**
 * Le barème d'une partie live (CLAUDE.md, section 6.5 ; ADR 0023) : la justesse d'abord, la rapidité ensuite. Une
 * réponse juste rapporte {@link #CORRECT} points, plus un bonus proportionnel au temps qui restait au chrono ; une
 * réponse fausse ou absente ne rapporte rien. Le mode live est le seul contexte compétitif (section 2.4) : la vitesse
 * y compte, contrairement à la révision personnelle.
 */
public final class LiveScoring {

    public static final int CORRECT = 500;
    public static final int MAX_SPEED_BONUS = 500;

    private LiveScoring() {}

    /**
     * @param taken le temps mis à répondre, depuis que la question a été posée
     * @param allowed la durée du chrono
     */
    public static int points(boolean correct, Duration taken, Duration allowed) {
        if (!correct) {
            return 0;
        }
        double remaining = 1 - (double) taken.toMillis() / allowed.toMillis();
        return CORRECT + (int) Math.round(MAX_SPEED_BONUS * Math.clamp(remaining, 0, 1));
    }
}
