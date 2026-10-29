package com.recallio.user.domain;

import java.util.Optional;

public interface UserProfileRepository {

    Optional<UserProfile> findBySubject(String oidcSubject);

    /**
     * Enregistre le profil s'il n'en existe pas encore pour ce {@code sub}, puis renvoie le profil en base. Sûr en cas
     * de premières connexions simultanées (deux onglets) : un seul profil est créé.
     */
    UserProfile createIfAbsent(UserProfile candidate);

    void update(UserProfile profile);
}
