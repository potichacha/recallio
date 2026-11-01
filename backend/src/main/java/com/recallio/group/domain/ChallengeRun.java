package com.recallio.group.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * La partie d'un membre dans un défi : les cartes qui lui ont été présentées, dans l'ordre, et ses réponses. Les cartes
 * se jouent l'une après l'autre : au plus une est en attente de réponse, toujours la dernière présentée.
 */
public record ChallengeRun(Challenge challenge, List<Attempt> attempts) {

    public ChallengeRun {
        attempts = attempts.stream()
                .sorted(Comparator.comparingInt(Attempt::position))
                .toList();
    }

    /** La carte présentée qui attend encore sa réponse. */
    public Optional<Attempt> pending() {
        return attempts.isEmpty() || attempts.getLast().isAnswered()
                ? Optional.empty()
                : Optional.of(attempts.getLast());
    }

    public boolean isStarted() {
        return !attempts.isEmpty();
    }

    public boolean isFinished() {
        return attempts.size() == challenge.cardCount() && pending().isEmpty();
    }

    /** La position de la prochaine carte à présenter. */
    public int nextPosition() {
        return attempts.size();
    }

    public int score() {
        return (int) attempts.stream().filter(Attempt::isCorrect).count();
    }

    /** Le temps passé à répondre, carte par carte : il départage deux scores égaux. */
    public Duration time() {
        return attempts.stream()
                .filter(Attempt::isAnswered)
                .map(attempt -> Duration.between(attempt.presentedAt(), attempt.answeredAtOrPresented()))
                .reduce(Duration.ZERO, Duration::plus);
    }

    /**
     * @param correct {@code null} tant que la carte n'a pas de réponse
     * @param timedOut la réponse est arrivée trop tard, ou n'est jamais arrivée : elle compte comme fausse
     */
    public record Attempt(
            int position,
            Instant presentedAt,
            @Nullable Instant answeredAt,
            @Nullable Boolean correct,
            boolean timedOut) {

        public boolean isAnswered() {
            return answeredAt != null;
        }

        public boolean isCorrect() {
            return Boolean.TRUE.equals(correct);
        }

        Instant answeredAtOrPresented() {
            return answeredAt == null ? presentedAt : answeredAt;
        }
    }
}
