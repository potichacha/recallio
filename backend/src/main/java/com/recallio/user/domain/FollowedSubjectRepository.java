package com.recallio.user.domain;

import com.recallio.user.LearningTrack;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Les sujets suivis par chaque utilisateur, désignés par leur slug (ADR 0014). */
public interface FollowedSubjectRepository {

    Set<String> findSlugs(UUID userId);

    /** Le parcours de chaque sujet suivi, par slug (ADR 0032). */
    Map<String, LearningTrack> findTracks(UUID userId);

    /** @return {@code false} si l'utilisateur ne suit pas ce sujet */
    boolean updateTrack(UUID userId, String slug, LearningTrack track);

    /**
     * Remplace les sujets suivis par {@code slugs} : les sujets déjà suivis gardent leur date de suivi, les autres sont
     * ajoutés avec {@code now}, et ceux qui n'y figurent plus sont retirés.
     */
    void replace(UUID userId, Set<String> slugs, Instant now);
}
