package com.recallio.gamification.domain.beta;

import java.util.Objects;

/**
 * Une carte souvent ratée sur la fenêtre (ADR 0076) : une question trop dure, mal posée, ou fausse.
 *
 * @param answers les réponses données à la carte, au moins {@link BetaReport#MIN_ANSWERS}
 * @param failures celles qui sont fausses
 * @param openReports ses signalements qui ne sont pas encore traités
 */
public record FailedCard(CardLabel card, long answers, long failures, long openReports) {

    public FailedCard {
        if (answers <= 0 || failures < 0 || failures > answers) {
            throw new IllegalArgumentException(
                    "%d réponse(s) fausse(s) sur %d : une carte ratée a des réponses".formatted(failures, answers));
        }
    }

    /** La part des réponses fausses, en pourcentage arrondi vers le bas. */
    public int failurePercent() {
        return Objects.requireNonNull(BetaReport.percent(failures, answers));
    }
}
