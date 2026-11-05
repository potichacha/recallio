package com.recallio.review.domain.exam;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Un examen blanc (ADR 0044) : des questions corrigées par le serveur, dans un temps limité, comme un entretien. Il ne
 * touche ni au journal de révision ni à FSRS : seule sa note est gardée.
 *
 * @param cardIds les questions, dans l'ordre où elles sont posées
 * @param deadline la fin du temps imparti ; une copie rendue après ({@link #GRACE} compris) est notée, mais hors délai
 * @param submission la copie rendue ; {@code null} tant qu'elle ne l'est pas
 */
public record MockExam(
        UUID id,
        UUID userId,
        String subjectSlug,
        List<UUID> cardIds,
        Instant startedAt,
        Instant deadline,
        @Nullable Submission submission) {

    /** Le nombre de questions d'un examen, s'il y en a assez dans le sujet. */
    public static final int QUESTIONS = 20;

    /** En dessous, le sujet n'a pas assez de questions corrigées par le serveur pour un examen. */
    public static final int MIN_QUESTIONS = 5;

    /** Le temps accordé par question. */
    public static final Duration TIME_PER_QUESTION = Duration.ofMinutes(1);

    /** La marge laissée au réseau : une copie envoyée à la dernière seconde arrive un peu après. */
    public static final Duration GRACE = Duration.ofSeconds(30);

    public MockExam {
        cardIds = List.copyOf(cardIds);
        if (cardIds.isEmpty()) {
            throw new IllegalArgumentException("Un examen a au moins une question");
        }
    }

    /** Un examen qui commence maintenant, avec le temps de ses questions. */
    public static MockExam start(UUID id, UUID userId, String subjectSlug, List<UUID> cardIds, Instant now) {
        Instant deadline = now.plus(TIME_PER_QUESTION.multipliedBy(cardIds.size()));
        return new MockExam(id, userId, subjectSlug, cardIds, now, deadline, null);
    }

    public boolean isSubmitted() {
        return submission != null;
    }

    /** Le temps qui reste ; nul une fois le temps écoulé. */
    public Duration remaining(Instant now) {
        return now.isBefore(deadline) ? Duration.between(now, deadline) : Duration.ZERO;
    }

    /** Vrai si une copie rendue à cet instant l'est dans le temps imparti, marge comprise. */
    public boolean inTime(Instant submittedAt) {
        return !submittedAt.isAfter(deadline.plus(GRACE));
    }

    /**
     * @param correct les questions justes
     * @param total les questions notées : celles de l'examen, sauf une carte retirée entre-temps
     */
    public record Submission(Instant submittedAt, int correct, int total) {

        public Submission {
            if (correct < 0 || correct > total) {
                throw new IllegalArgumentException("%d juste(s) sur %d".formatted(correct, total));
            }
        }

        /** La note, de 0 à 100. */
        public int percent() {
            return total == 0 ? 0 : Math.round(correct * 100f / total);
        }
    }
}
