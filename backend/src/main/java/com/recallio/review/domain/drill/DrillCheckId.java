package com.recallio.review.domain.drill;

import com.recallio.platform.domain.StableUuid;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * L'empreinte d'une réponse de la révision intensive (ADR 0079) : l'identifiant de requête du client, la carte et les
 * choix cochés, hachés ensemble. C'est ce que le compteur du jour retient pour ne pas compter deux fois une requête
 * rejouée.
 *
 * <p>La carte et les choix y entrent pour que l'identifiant ne serve qu'à une réponse : le réutiliser pour une autre
 * carte, ou pour d'autres choix, donne une autre empreinte, et la réponse est comptée. L'empreinte ne se lit pas : ni la
 * carte ni la réponse ne sont gardées (ADR 0078).
 */
public final class DrillCheckId {

    /** Les empreintes gardées par jour : un renvoi suit sa requête de près, les plus anciennes ne servent plus. */
    public static final int KEPT_PER_DAY = 32;

    private static final String NAMESPACE = "recallio:drill-check";

    private DrillCheckId() {}

    public static UUID of(UUID requestId, UUID cardId, Set<Integer> selectedChoices) {
        return StableUuid.of(NAMESPACE, requestId + "|" + cardId + "|" + new TreeSet<>(selectedChoices));
    }
}
