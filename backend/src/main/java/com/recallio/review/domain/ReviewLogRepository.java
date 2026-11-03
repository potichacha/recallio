package com.recallio.review.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Journal de révision append-only. */
public interface ReviewLogRepository {

    /**
     * Ajoute l'entrée si aucune n'existe déjà pour son {@code requestId}.
     *
     * @return {@code false} si une autre requête portant le même identifiant l'a déjà enregistrée
     */
    boolean appendIfAbsent(ReviewLogEntry entry);

    Optional<ReviewLogEntry> findByRequestId(UUID requestId);

    /** Les cartes que l'utilisateur a révisées depuis cet instant (le début de sa journée, par exemple). */
    Set<UUID> cardsReviewedSince(UUID userId, Instant since);
}
