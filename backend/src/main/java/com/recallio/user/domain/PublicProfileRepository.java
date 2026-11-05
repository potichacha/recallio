package com.recallio.user.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Les profils publics (ADR 0043), dans la table {@code public_profile}. */
public interface PublicProfileRepository {

    Optional<PublicProfileSettings> findByUser(UUID userId);

    Optional<PublicProfileSettings> findByHandle(PublicHandle handle);

    /** Crée ou remplace les réglages de l'utilisateur. */
    void save(PublicProfileSettings settings, Instant updatedAt);
}
