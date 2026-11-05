package com.recallio.user;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** API publique du module user (ADR 0001) : ce que les autres modules peuvent afficher d'un utilisateur. */
public interface UserDirectory {

    /** Le nom affiché de chaque utilisateur connu parmi {@code userIds} ; un identifiant inconnu est absent du résultat. */
    Map<UUID, String> displayNames(Collection<UUID> userIds);

    /** L'utilisateur et son fuseau, s'il existe : pour un traitement qui ne vient pas d'une requête, comme un rappel. */
    Optional<UserRef> user(UUID userId);
}
