package com.recallio.live.domain;

import com.recallio.live.domain.LiveExceptions.LiveGameFullException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Une partie live (CLAUDE.md, section 6.5 ; ADR 0021) : un hôte, membre du groupe, l'ouvre sur un sujet du groupe ; les
 * membres la rejoignent. Elle vit en mémoire et se partage entre les connexions de ses joueurs, d'où les méthodes
 * {@code synchronized}.
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
    private final LiveGameStatus status = LiveGameStatus.LOBBY;
    private final Map<UUID, Player> players = new LinkedHashMap<>();

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

    /**
     * Une connexion de plus pour ce joueur ; il entre dans la partie à sa première connexion.
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

    /** Une connexion de moins ; le joueur quitte la partie avec sa dernière connexion. */
    public synchronized Snapshot leave(UUID userId) {
        Player current = players.get(userId);
        if (current != null) {
            if (current.connections() > 1) {
                players.put(userId, current.withConnections(current.connections() - 1));
            } else {
                players.remove(userId);
            }
        }
        return snapshot();
    }

    /** Vrai si plus personne n'est dans la partie. */
    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }

    /** Vide depuis son ouverture, au-delà de {@link #ABANDONED_AFTER} : elle ne bloque plus le groupe. */
    public synchronized boolean isAbandonedAt(Instant now) {
        return players.isEmpty() && !now.isBefore(createdAt.plus(ABANDONED_AFTER));
    }

    /** L'état de la partie, figé : ce qui est diffusé aux joueurs. */
    public synchronized Snapshot snapshot() {
        List<PlayerView> views = players.values().stream()
                .map(player -> new PlayerView(
                        player.userId(), player.displayName(), player.userId().equals(hostId)))
                .toList();
        return new Snapshot(id, groupId, subjectSlug, hostId, createdAt, status, views);
    }

    /** @param connections les connexions ouvertes du joueur (onglets, appareils) */
    private record Player(UUID userId, String displayName, Instant joinedAt, int connections) {

        Player withConnections(int count) {
            return new Player(userId, displayName, joinedAt, count);
        }
    }

    /** @param players dans l'ordre d'arrivée */
    public record Snapshot(
            UUID id,
            UUID groupId,
            String subjectSlug,
            UUID hostId,
            Instant createdAt,
            LiveGameStatus status,
            List<PlayerView> players) {

        public Snapshot {
            players = List.copyOf(players);
        }
    }

    public record PlayerView(UUID userId, String displayName, boolean host) {}
}
