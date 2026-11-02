package com.recallio.live.domain;

import java.util.List;
import java.util.UUID;

/** Les résultats des parties finies : seule trace durable du mode live, dont les parties vivent en mémoire. */
public interface LiveResultRepository {

    /** Enregistre le résultat ; sans effet s'il l'est déjà. */
    void save(LiveGameResult result);

    /** Les dernières parties du groupe, la plus récente d'abord, au plus {@code limit}. */
    List<LiveGameResult> recentOfGroup(UUID groupId, int limit);
}
