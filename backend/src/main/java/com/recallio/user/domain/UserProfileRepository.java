package com.recallio.user.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {

    Optional<UserProfile> findBySubject(String oidcSubject);

    Optional<UserProfile> findById(UUID id);

    /**
     * Enregistre le profil s'il n'en existe pas encore pour ce {@code sub}, puis renvoie le profil en base. Sûr en cas
     * de premières connexions simultanées (deux onglets) : un seul profil est créé.
     */
    UserProfile createIfAbsent(UserProfile candidate);

    void update(UserProfile profile);

    /** Le nom affiché de chacun des profils demandés qui existent. */
    Map<UUID, DisplayName> findDisplayNames(Collection<UUID> ids);
}
