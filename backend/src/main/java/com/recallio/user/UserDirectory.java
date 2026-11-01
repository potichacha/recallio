package com.recallio.user;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** API publique du module user (ADR 0001) : ce que les autres modules peuvent afficher d'un utilisateur. */
public interface UserDirectory {

    /** Le nom affiché de chaque utilisateur connu parmi {@code userIds} ; un identifiant inconnu est absent du résultat. */
    Map<UUID, String> displayNames(Collection<UUID> userIds);
}
