package com.recallio.ranked.domain;

import com.recallio.ranked.domain.glicko.Rating;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Un duel du classé (ADR 0046) : deux joueurs, les mêmes questions corrigées par le serveur, un chrono par question
 * mesuré par le serveur. En asynchrone, chacun joue quand il veut ; en direct, les deux en même temps.
 *
 * @param cardIds les questions, dans l'ordre où elles sont servies
 * @param expiresAt ouvert, le duel attend un adversaire jusque-là ; complet, chacun doit avoir fini avant
 * @param players le joueur qui a ouvert le duel (place 0), puis son adversaire (place 1)
 */
public record Duel(
        UUID id,
        String subjectSlug,
        Mode mode,
        Status status,
        Season season,
        List<UUID> cardIds,
        Instant createdAt,
        Instant expiresAt,
        @Nullable Instant finishedAt,
        List<DuelPlayer> players) {

    public static final int QUESTIONS = 5;
    public static final Duration TIME_PER_QUESTION = Duration.ofSeconds(30);

    /** La marge laissée au réseau : une réponse partie à la dernière seconde arrive un peu après. */
    public static final Duration GRACE = Duration.ofSeconds(2);

    /** Le temps qu'un duel asynchrone ouvert attend un adversaire. */
    public static final Duration OPEN_FOR = Duration.ofHours(48);

    /** Le temps laissé à chacun pour finir un duel asynchrone, une fois l'adversaire arrivé. */
    public static final Duration TIME_TO_PLAY = Duration.ofHours(24);

    /** La durée d'un duel en direct : bien plus que ses questions, pour les déconnexions. */
    public static final Duration LIVE_FOR = Duration.ofMinutes(5);

    public enum Mode {
        ASYNC,
        LIVE
    }

    public enum Status {
        /** Le joueur qui l'a ouvert attend un adversaire, et peut déjà jouer. */
        OPEN,
        /** Deux joueurs ; l'un au moins n'a pas fini. */
        FULL,
        FINISHED,
        /** Personne n'est venu à temps : aucune cote ne change. */
        EXPIRED
    }

    public Duel {
        cardIds = List.copyOf(cardIds);
        players = List.copyOf(players);
    }

    public Optional<DuelPlayer> player(UUID userId) {
        return players.stream().filter(player -> player.userId().equals(userId)).findFirst();
    }

    public Optional<DuelPlayer> opponentOf(UUID userId) {
        return players.stream()
                .filter(player -> !player.userId().equals(userId))
                .findFirst();
    }

    public boolean isPastDeadline(Instant now) {
        return now.isAfter(expiresAt);
    }

    /** Vrai si le duel attend encore des réponses. */
    public boolean isPlayable() {
        return status == Status.OPEN || status == Status.FULL;
    }

    /**
     * Un joueur du duel.
     *
     * @param ratingBefore sa cote quand il est entré dans le duel : son adversaire joue contre elle
     * @param answers les questions qui lui ont été servies, dans l'ordre
     * @param finishedAt quand il a répondu à la dernière ; {@code null} avant
     * @param ratingAfter sa cote après le duel ; {@code null} tant qu'il n'est pas réglé
     */
    public record DuelPlayer(
            UUID userId,
            int seat,
            Rating ratingBefore,
            Instant joinedAt,
            List<DuelAnswer> answers,
            @Nullable Instant finishedAt,
            @Nullable Double ratingAfter) {

        public DuelPlayer {
            answers = List.copyOf(answers);
        }

        public boolean hasFinished() {
            return finishedAt != null;
        }

        /** La question servie et pas encore répondue, s'il y en a une. */
        public Optional<DuelAnswer> pending() {
            return answers.stream()
                    .filter(answer -> answer.answeredAt() == null)
                    .findFirst();
        }

        public DuelScore score(int questions) {
            return DuelScore.of(answers, questions);
        }
    }

    /**
     * Une question servie à un joueur.
     *
     * @param answeredAt {@code null} tant qu'il n'a pas répondu ; une question dont le temps est passé reste ainsi
     */
    public record DuelAnswer(
            int position, Instant servedAt, @Nullable Instant answeredAt, boolean correct) {

        /** Le temps compté : celui de la réponse, plafonné au temps imparti ; tout le temps imparti sans réponse. */
        public Duration time() {
            Instant answered = answeredAt;
            if (answered == null) {
                return TIME_PER_QUESTION;
            }
            Duration taken = Duration.between(servedAt, answered);
            return taken.compareTo(TIME_PER_QUESTION) > 0 ? TIME_PER_QUESTION : taken;
        }

        /** Vrai si le temps de la question est passé, marge comprise. */
        public boolean isLate(Instant now) {
            return now.isAfter(servedAt.plus(TIME_PER_QUESTION).plus(GRACE));
        }
    }
}
