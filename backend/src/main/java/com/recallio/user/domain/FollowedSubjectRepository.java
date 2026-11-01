package com.recallio.user.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Les sujets suivis par chaque utilisateur, désignés par leur slug (ADR 0014). */
public interface FollowedSubjectRepository {

    Set<String> findSlugs(UUID userId);

    /**
     * Remplace les sujets suivis par {@code slugs} : les sujets déjà suivis gardent leur date de suivi, les autres sont
     * ajoutés avec {@code now}, et ceux qui n'y figurent plus sont retirés.
     */
    void replace(UUID userId, Set<String> slugs, Instant now);
}
