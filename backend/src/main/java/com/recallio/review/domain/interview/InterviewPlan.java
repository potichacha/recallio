package com.recallio.review.domain.interview;

import com.recallio.review.domain.interview.InterviewExceptions.InvalidInterviewDateException;
import java.time.LocalDate;

/**
 * L'entretien prévu dans un sujet suivi (ADR 0066), tel qu'il est gardé : sa date, le jour où elle a été fixée et la
 * ligne de départ. Le reste du compte à rebours se recalcule à chaque lecture ({@link InterviewCountdown}).
 *
 * @param interviewOn le jour de l'entretien
 * @param plannedOn le jour, dans le fuseau de l'utilisateur, où la date a été fixée ou changée : la semaine 1 commence
 *     ce jour-là
 * @param notionsToAcquire la ligne de départ : les notions essentielles du sujet pas encore acquises ce jour-là
 */
public record InterviewPlan(String subjectSlug, LocalDate interviewOn, LocalDate plannedOn, int notionsToAcquire) {

    /** Un entretien se prévoit au plus un an à l'avance. */
    public static final int MAX_DAYS_AHEAD = 365;

    /**
     * Vérifie la date d'un entretien qu'on fixe aujourd'hui.
     *
     * @throws InvalidInterviewDateException si elle n'est pas après aujourd'hui, ou plus d'un an après
     */
    public static void requireValidDate(LocalDate interviewOn, LocalDate today) {
        if (!interviewOn.isAfter(today) || interviewOn.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new InvalidInterviewDateException(MAX_DAYS_AHEAD);
        }
    }
}
