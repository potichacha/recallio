package com.recallio.ranked;

import java.util.Map;
import java.util.UUID;

/**
 * API publique du module ranked (ADR 0001) : le rang d'un utilisateur dans chaque sujet, pour son profil public (ADR
 * 0043). Un rang est vérifié : il vient de duels corrigés par le serveur. Les cotes, elles, ne sortent pas du module.
 */
public interface RankedProfiles {

    /** Le rang de l'utilisateur par sujet, dans la saison en cours ; un sujet en placement ou jamais joué est absent. */
    Map<String, PublicRank> ranks(UUID userId);

    /**
     * @param tier BRONZE, SILVER, GOLD, PLATINUM, DIAMOND ou MASTER
     * @param division de 4 (la plus basse) à 1 ; 0 pour MASTER
     */
    record PublicRank(String tier, int division) {}
}
