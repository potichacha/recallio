package com.recallio.review.domain.drill;

import com.recallio.platform.domain.ForbiddenException;

/**
 * Le compte, sans Plus, a fait corriger toutes ses réponses du jour en révision intensive (ADR 0079). Un 403 et non un
 * 429 : réessayer dans un instant n'y change rien, c'est un droit qui manque.
 */
public class DrillLimitReachedException extends ForbiddenException {

    public static final String CODE = "review.drill-daily-limit-reached";

    public DrillLimitReachedException(int dailyLimit) {
        super(
                CODE,
                ("Tu as fait tes %d questions gratuites d'aujourd'hui. Reviens demain, ou passe à Plus pour t'entraîner"
                                + " sans limite.")
                        .formatted(dailyLimit));
    }
}
