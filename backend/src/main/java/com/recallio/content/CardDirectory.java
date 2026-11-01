package com.recallio.content;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * API publique du module content pour les autres modules (ADR 0001) : ils référencent des cartes par leur identifiant
 * stable, sans connaître les tables ni les classes internes du module. Seules les cartes actives des sujets publiés
 * sont visibles.
 */
public interface CardDirectory {

    /**
     * Les cartes actives qui correspondent au filtre, dans l'ordre du programme (sujet, thème, position).
     *
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le thème demandé n'existe pas
     */
    List<ActiveCard> activeCards(CardFilter filter);

    /** La version courante des cartes actives demandées, dans l'ordre de {@code cardIds}. */
    List<CardSnapshot> currentVersions(Collection<UUID> cardIds);

    /** Tous les thèmes publiés, dans l'ordre du programme, avec les identifiants de leurs cartes actives. */
    List<ThemeCards> activeCardsByTheme();

    default Optional<CardSnapshot> activeCard(UUID cardId) {
        return currentVersions(List.of(cardId)).stream().findFirst();
    }

    /**
     * @param subjectSlugs restreint à ces sujets (un ensemble vide ne laisse passer aucune carte), ou tous les sujets si
     *     {@code null}
     * @param themeSlug restreint à un thème, ou tous les thèmes si {@code null}
     * @param type restreint à un format, ou tous les formats si {@code null}
     */
    record CardFilter(
            @Nullable Set<String> subjectSlugs,
            @Nullable String themeSlug,
            @Nullable CardType type) {

        public CardFilter {
            subjectSlugs = subjectSlugs == null ? null : Set.copyOf(subjectSlugs);
        }

        public static CardFilter all() {
            return new CardFilter(null, null, null);
        }
    }

    /** Une carte active et son sujet, pour répartir les nouvelles cartes entre les sujets. */
    record ActiveCard(UUID cardId, String subjectSlug) {}

    record ThemeCards(String subjectSlug, String subjectTitle, String slug, String title, List<UUID> cardIds) {

        public ThemeCards {
            cardIds = List.copyOf(cardIds);
        }
    }

    /** Une carte dans sa version courante : {@code versionId} est l'identifiant de la version révisée. */
    record CardSnapshot(
            UUID cardId,
            UUID versionId,
            CardType type,
            String subjectSlug,
            String subjectTitle,
            String themeSlug,
            String themeTitle,
            String prompt,
            String answer) {}
}
