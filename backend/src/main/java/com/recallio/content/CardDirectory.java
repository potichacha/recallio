package com.recallio.content;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * API publique du module content pour les autres modules (ADR 0001) : ils référencent des cartes par leur identifiant
 * stable, sans connaître les tables ni les classes internes du module. Seules les cartes actives sont visibles.
 */
public interface CardDirectory {

    /**
     * Identifiants des cartes actives qui correspondent au filtre, dans l'ordre du programme (thème, puis position).
     *
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le thème demandé n'existe pas
     */
    List<UUID> activeCardIds(CardFilter filter);

    /** La version courante des cartes actives demandées, dans l'ordre de {@code cardIds}. */
    List<CardSnapshot> currentVersions(Collection<UUID> cardIds);

    default Optional<CardSnapshot> activeCard(UUID cardId) {
        return currentVersions(List.of(cardId)).stream().findFirst();
    }

    /**
     * @param themeSlug restreint à un thème, ou tous les thèmes si {@code null}
     * @param type restreint à un format, ou tous les formats si {@code null}
     */
    record CardFilter(@Nullable String themeSlug, @Nullable CardType type) {

        public static CardFilter all() {
            return new CardFilter(null, null);
        }
    }

    /** Une carte dans sa version courante : {@code versionId} est l'identifiant de la version révisée. */
    record CardSnapshot(
            UUID cardId,
            UUID versionId,
            CardType type,
            String themeSlug,
            String themeTitle,
            String prompt,
            String answer) {}
}
