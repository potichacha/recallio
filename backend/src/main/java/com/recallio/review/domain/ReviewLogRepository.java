package com.recallio.review.domain;

import java.util.Optional;
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
}
