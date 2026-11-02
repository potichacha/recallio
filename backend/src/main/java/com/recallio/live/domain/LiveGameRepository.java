package com.recallio.live.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Les parties ouvertes. En mémoire pour une seule instance du backend (ADR 0021) ; un stockage partagé (Valkey)
 * prendrait sa place si l'application passait à plusieurs instances.
 */
public interface LiveGameRepository {

    /**
     * Enregistre la partie, sauf si son groupe en a déjà une ouverte : la vérification et l'enregistrement sont
     * atomiques, deux lancements simultanés ne créent pas deux parties.
     *
     * @return la partie déjà ouverte du groupe, s'il y en avait une (la nouvelle n'est alors pas enregistrée)
     */
    Optional<LiveGame> registerUnlessGroupHasOne(LiveGame game);

    Optional<LiveGame> findById(UUID gameId);

    Optional<LiveGame> findOpenByGroup(UUID groupId);

    /** Ferme la partie : elle n'est plus trouvée, et son groupe peut en ouvrir une autre. */
    void remove(LiveGame game);
}
