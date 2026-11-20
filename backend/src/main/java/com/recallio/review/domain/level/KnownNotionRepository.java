package com.recallio.review.domain.level;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Les notions qu'un utilisateur a montré connaître au test de niveau (ADR 0068). Une notion connue le reste jusqu'à ce
 * qu'il annule le test de son sujet.
 */
public interface KnownNotionRepository {

    /** Rend connues ces notions d'un sujet ; une notion déjà connue garde sa date. */
    void markKnown(UUID userId, String subjectSlug, Collection<UUID> notionIds, Instant knownAt);

    /** Les notions connues de l'utilisateur dans ces sujets. */
    Set<UUID> known(UUID userId, Collection<String> subjectSlugs);

    /** Oublie toutes ses notions connues d'un sujet : il annule son test de niveau, s'il en avait passé un. */
    void forget(UUID userId, String subjectSlug);
}
