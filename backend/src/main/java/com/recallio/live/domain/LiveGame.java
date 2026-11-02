package com.recallio.live.domain;

import com.recallio.live.domain.LiveExceptions.HostOnlyException;
import com.recallio.live.domain.LiveExceptions.LiveGameFullException;
import com.recallio.live.domain.LiveExceptions.LiveGameStartedException;
import com.recallio.live.domain.LiveExceptions.NotInGameException;
import com.recallio.live.domain.LiveExceptions.RoundClosedException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Une partie live (CLAUDE.md, section 6.5 ; ADR 0021 et 0022) : un hôte, membre du groupe, l'ouvre sur un sujet du
 * groupe ; les membres la rejoignent ; l'hôte la démarre, et la même question est posée à tous au même moment.
 *
 * <p>Le déroulé est une machine à états pilotée par le temps : {@code LOBBY} → ({@code QUESTION} → {@code REVEAL}) × N
 * → {@code FINISHED}. Chaque méthode reçoit l'instant présent : la partie ne lit jamais d'horloge, ce qui la rend
 * testable. Elle se partage entre les connexions de ses joueurs et le minuteur, d'où les méthodes {@code synchronized}.
 *
 * <p>Un joueur peut avoir plusieurs connexions (deux onglets) : il reste dans la partie tant qu'il en garde une.
 */
public final class LiveGame {

    /** Au-delà, le classement en direct n'est plus lisible et la diffusion coûte cher. */
    public static final int MAX_PLAYERS = 50;

    /** Une salle restée vide aussi longtemps est abandonnée : l'hôte l'a ouverte sans jamais la rejoindre. */
    public static final Duration ABANDONED_AFTER = Duration.ofMinutes(5);

    private final UUID id;
    private final UUID groupId;
    private final String subjectSlug;
    private final UUID hostId;
    private final Instant createdAt;
    private final Map<UUID, Player> players = new LinkedHashMap<>();
    /** Les réponses de chaque question, par joueur. */
    private final List<Map<UUID, Answer>> answers = new ArrayList<>();

    private LiveGameStatus status = LiveGameStatus.LOBBY;
    private List<LiveQuestion> questions = List.of();
    private @Nullable LiveTiming timing;
    private int round = -1;
    private @Nullable Instant phaseEndsAt;

    public LiveGame(UUID id, UUID groupId, String subjectSlug, UUID hostId, Instant createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.subjectSlug = subjectSlug;
        this.hostId = hostId;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID groupId() {
        return groupId;
    }

    public String subjectSlug() {
        return subjectSlug;
    }

    /**
     * Une connexion de plus pour ce joueur ; il entre dans la partie à sa première connexion, même en cours de partie.
     *
     * @throws LiveGameFullException si la partie compte déjà {@link #MAX_PLAYERS} joueurs
     */
    public synchronized Snapshot join(UUID userId, String displayName, Instant now) {
        Player current = players.get(userId);
        if (current == null && players.size() >= MAX_PLAYERS) {
            throw new LiveGameFullException(MAX_PLAYERS);
        }
        players.put(
                userId,
                current == null
                        ? new Player(userId, displayName, now, 1)
                        : current.withConnections(current.connections() + 1));
        return snapshot();
    }

    /**
     * Une connexion de moins ; le joueur quitte la partie avec sa dernière connexion. Si tous ceux qui restent ont déjà
     * répondu, la correction est révélée sans attendre.
     */
    public synchronized Snapshot leave(UUID userId, Instant now) {
        Player current = players.get(userId);
        if (current != null) {
            if (current.connections() > 1) {
                players.put(userId, current.withConnections(current.connections() - 1));
            } else {
                players.remove(userId);
                revealIfEveryoneAnswered(now);
            }
        }
        return snapshot();
    }

    /**
     * L'hôte démarre la partie : la première question est posée à tous.
     *
     * @throws HostOnlyException si {@code userId} n'est pas l'hôte
     * @throws LiveGameStartedException si la partie a déjà démarré
     */
    public synchronized Snapshot start(UUID userId, List<LiveQuestion> series, LiveTiming rhythm, Instant now) {
        if (!userId.equals(hostId)) {
            throw new HostOnlyException();
        }
        if (status != LiveGameStatus.LOBBY) {
            throw new LiveGameStartedException();
        }
        if (series.isEmpty()) {
            throw new IllegalArgumentException("Une partie a au moins une question");
        }
        questions = List.copyOf(series);
        timing = rhythm;
        ask(0, now);
        return snapshot();
    }

    /**
     * Enregistre la réponse d'un joueur à la question en cours. Seule la première compte : la renvoyer ne change rien.
     * Quand tous les joueurs présents ont répondu, la correction est révélée sans attendre la fin du chrono.
     *
     * @throws NotInGameException si l'utilisateur n'est pas connecté à la partie
     * @throws RoundClosedException si la question n'attend plus de réponse (chrono écoulé, ou autre question)
     */
    public synchronized Snapshot answer(UUID userId, int questionIndex, Set<Integer> selected, Instant now) {
        if (!players.containsKey(userId)) {
            throw new NotInGameException();
        }
        Instant endsAt = phaseEndsAt;
        if (status != LiveGameStatus.QUESTION || questionIndex != round || endsAt == null || now.isAfter(endsAt)) {
            throw new RoundClosedException();
        }
        answers.get(round)
                .putIfAbsent(
                        userId,
                        new Answer(Set.copyOf(selected), questions.get(round).isCorrect(selected), now));
        revealIfEveryoneAnswered(now);
        return snapshot();
    }

    /**
     * Fait avancer la partie si l'étape en cours est arrivée à son terme : question → correction → question suivante →
     * … → fin. Sans effet avant l'échéance : un réveil du minuteur en double ou en avance ne change rien.
     *
     * @return vrai si la partie a changé d'étape
     */
    public synchronized boolean advance(Instant now) {
        Instant endsAt = phaseEndsAt;
        if (endsAt == null || now.isBefore(endsAt)) {
            return false;
        }
        if (status == LiveGameStatus.QUESTION) {
            reveal(now);
        } else if (round + 1 < questions.size()) {
            ask(round + 1, now);
        } else {
            status = LiveGameStatus.FINISHED;
            phaseEndsAt = null;
        }
        return true;
    }

    /** L'échéance de l'étape en cours ; aucune en salle d'attente ni une fois la partie finie. */
    public synchronized @Nullable Instant phaseEndsAt() {
        return phaseEndsAt;
    }

    /** Vrai si plus personne n'est dans la partie. */
    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }

    /** La partie ne bloque plus son groupe : finie, ou salle restée vide au-delà de {@link #ABANDONED_AFTER}. */
    public synchronized boolean isOverAt(Instant now) {
        return status == LiveGameStatus.FINISHED
                || (players.isEmpty() && !now.isBefore(createdAt.plus(ABANDONED_AFTER)));
    }

    /** L'état de la partie, figé : ce qui est diffusé aux joueurs. */
    public synchronized Snapshot snapshot() {
        Map<UUID, Answer> current = round >= 0 ? answers.get(round) : Map.of();
        List<PlayerView> views = players.values().stream()
                .map(player -> new PlayerView(
                        player.userId(),
                        player.displayName(),
                        player.userId().equals(hostId),
                        current.containsKey(player.userId())))
                .toList();
        return new Snapshot(id, groupId, subjectSlug, hostId, createdAt, status, views, currentRound(current));
    }

    private @Nullable Round currentRound(Map<UUID, Answer> current) {
        if (round < 0) {
            return null;
        }
        int correct = (int) current.values().stream().filter(Answer::correct).count();
        return new Round(round, questions.size(), questions.get(round), phaseEndsAt, current.size(), correct);
    }

    private void ask(int index, Instant now) {
        round = index;
        answers.add(new HashMap<>());
        status = LiveGameStatus.QUESTION;
        phaseEndsAt = now.plus(requireTiming().question());
    }

    private void reveal(Instant now) {
        status = LiveGameStatus.REVEAL;
        phaseEndsAt = now.plus(requireTiming().reveal());
    }

    private void revealIfEveryoneAnswered(Instant now) {
        if (status == LiveGameStatus.QUESTION
                && !players.isEmpty()
                && answers.get(round).keySet().containsAll(players.keySet())) {
            reveal(now);
        }
    }

    private LiveTiming requireTiming() {
        LiveTiming rhythm = timing;
        if (rhythm == null) {
            throw new IllegalStateException("La partie " + id + " n'a pas démarré");
        }
        return rhythm;
    }

    /** @param connections les connexions ouvertes du joueur (onglets, appareils) */
    private record Player(UUID userId, String displayName, Instant joinedAt, int connections) {

        Player withConnections(int count) {
            return new Player(userId, displayName, joinedAt, count);
        }
    }

    /** La réponse d'un joueur à une question ; son instant servira à récompenser la rapidité (étape suivante). */
    private record Answer(Set<Integer> selected, boolean correct, Instant answeredAt) {}

    /**
     * @param players dans l'ordre d'arrivée
     * @param round la question en cours, ou la dernière posée ; absente en salle d'attente
     */
    public record Snapshot(
            UUID id,
            UUID groupId,
            String subjectSlug,
            UUID hostId,
            Instant createdAt,
            LiveGameStatus status,
            List<PlayerView> players,
            @Nullable Round round) {

        public Snapshot {
            players = List.copyOf(players);
        }
    }

    /** @param answered vrai s'il a répondu à la question en cours */
    public record PlayerView(UUID userId, String displayName, boolean host, boolean answered) {}

    /**
     * @param index la position de la question, à partir de 0
     * @param endsAt la fin du chrono (question) ou de la correction (avant la suivante) ; absente une fois la partie
     *     finie
     * @param correctCount les bonnes réponses ; à ne montrer qu'à la correction
     */
    public record Round(
            int index,
            int total,
            LiveQuestion question,
            @Nullable Instant endsAt,
            int answeredCount,
            int correctCount) {}
}
