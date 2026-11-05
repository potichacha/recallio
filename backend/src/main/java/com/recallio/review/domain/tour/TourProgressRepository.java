package com.recallio.review.domain.tour;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** La progression d'un utilisateur dans le Tour (ADR 0037) : les notions qu'il a validées, et sa dernière page. */
public interface TourProgressRepository {

    /**
     * Garde la première validation : valider de nouveau ne change rien.
     *
     * @return vrai si la notion vient d'être validée pour la première fois
     */
    boolean markValidated(UUID userId, UUID notionId, Instant validatedAt);

    /** Toutes les notions que l'utilisateur a validées, tous sujets confondus. */
    long validatedCount(UUID userId);

    /** Celles de ces notions que l'utilisateur a validées. */
    Set<UUID> validated(UUID userId, Collection<UUID> notionIds);

    /** Remplace la dernière page que l'utilisateur a ouverte dans le Tour de ce sujet. */
    void saveLastPage(UUID userId, String subjectSlug, TourLastPage lastPage, Instant openedAt);

    Optional<TourLastPage> lastPage(UUID userId, String subjectSlug);
}
