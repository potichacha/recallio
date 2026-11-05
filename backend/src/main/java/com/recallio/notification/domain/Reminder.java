package com.recallio.notification.domain;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Le rappel du jour (ADR 0047) : au plus un par jour et par appareil, à partir de l'heure choisie, et seulement si
 * l'objectif du jour n'est pas encore atteint.
 *
 * @param url la page qu'ouvre la notification
 */
public record Reminder(String title, String body, String url) {

    static final String REVIEW_URL = "/review";

    /** Vrai si l'heure du rappel est passée dans la journée de l'utilisateur, et qu'il n'en a pas encore eu aujourd'hui. */
    public static boolean isDue(int reminderHour, ZonedDateTime localNow, @Nullable LocalDate lastRemindedOn) {
        return localNow.getHour() >= reminderHour && !localNow.toLocalDate().equals(lastRemindedOn);
    }

    /** Le message : la série en jeu s'il y en a une, sinon l'invitation à réviser. */
    public static Reminder forStreak(int streak) {
        if (streak > 0) {
            return new Reminder(
                    "🔥 Ta série de %d jour%s t'attend".formatted(streak, streak > 1 ? "s" : ""),
                    "Quelques cartes suffisent pour la garder aujourd'hui.",
                    REVIEW_URL);
        }
        return new Reminder(
                "Tes cartes du jour t'attendent", "Dix minutes de révision, et ta série démarre.", REVIEW_URL);
    }
}
