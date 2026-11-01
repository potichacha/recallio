package com.recallio.group.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Un défi de groupe (CLAUDE.md, section 6.4 ; ADR 0020) : la même série de QCM pour tous les membres, avec un délai de
 * réponse par carte, ouvert pendant une semaine.
 *
 * @param cardIds la série, dans l'ordre où elle est jouée
 */
public record Challenge(
        UUID id,
        UUID groupId,
        String title,
        List<UUID> cardIds,
        Duration perCard,
        UUID createdBy,
        Instant createdAt,
        Instant endsAt) {

    public static final int TITLE_MAX_LENGTH = 80;
    public static final int MIN_CARDS = 3;
    public static final int MAX_CARDS = 20;
    public static final int MIN_SECONDS = 10;
    public static final int MAX_SECONDS = 120;

    /** Le temps de jouer, sans qu'un défi oublié reste ouvert indéfiniment. */
    public static final Duration OPEN_FOR = Duration.ofDays(7);

    /**
     * Une réponse arrivée juste après la fin du délai est acceptée : le temps du trajet réseau ne doit pas faire perdre un
     * point sur mobile.
     */
    public static final Duration NETWORK_GRACE = Duration.ofSeconds(2);

    public Challenge {
        cardIds = List.copyOf(cardIds);
    }

    /** @throws InvalidGroupException si le titre, la taille de la série ou le délai sont hors des bornes */
    public static Challenge create(
            UUID id, UUID groupId, String title, List<UUID> cardIds, int secondsPerCard, UUID createdBy, Instant now) {
        String stripped = title.strip();
        if (stripped.isEmpty() || stripped.length() > TITLE_MAX_LENGTH) {
            throw new InvalidGroupException(
                    "group.invalid-challenge",
                    "Le titre du défi doit contenir entre 1 et " + TITLE_MAX_LENGTH + " caractères.");
        }
        if (cardIds.size() < MIN_CARDS || cardIds.size() > MAX_CARDS) {
            throw new InvalidGroupException(
                    "group.invalid-challenge", "Un défi compte entre " + MIN_CARDS + " et " + MAX_CARDS + " cartes.");
        }
        if (secondsPerCard < MIN_SECONDS || secondsPerCard > MAX_SECONDS) {
            throw new InvalidGroupException(
                    "group.invalid-challenge",
                    "Le délai par carte est compris entre " + MIN_SECONDS + " et " + MAX_SECONDS + " secondes.");
        }
        return new Challenge(
                id, groupId, stripped, cardIds, Duration.ofSeconds(secondsPerCard), createdBy, now, now.plus(OPEN_FOR));
    }

    public int cardCount() {
        return cardIds.size();
    }

    public boolean isOpenAt(Instant now) {
        return now.isBefore(endsAt);
    }

    /** L'heure limite affichée pour une carte présentée à {@code presentedAt}. */
    public Instant deadline(Instant presentedAt) {
        return presentedAt.plus(perCard);
    }

    /** Vrai si une réponse reçue à {@code answeredAt} compte encore (délai et tolérance réseau compris). */
    public boolean answeredInTime(Instant presentedAt, Instant answeredAt) {
        return !answeredAt.isAfter(deadline(presentedAt).plus(NETWORK_GRACE));
    }
}
