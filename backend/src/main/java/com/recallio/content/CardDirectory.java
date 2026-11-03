package com.recallio.content;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    /** La notion de chaque carte demandée qui en a une (ADR 0026) ; les cartes d'un document Markdown n'en ont pas. */
    Map<UUID, UUID> notionsOf(Collection<UUID> cardIds);

    /** Les notions actives de ces sujets publiés, dans l'ordre de leur référentiel (ADR 0032). */
    List<NotionInfo> notions(Collection<String> subjectSlugs);

    /** Tous les thèmes publiés, dans l'ordre du programme, avec les identifiants de leurs cartes actives. */
    List<ThemeCards> activeCardsByTheme();

    default Optional<CardSnapshot> activeCard(UUID cardId) {
        return currentVersions(List.of(cardId)).stream().findFirst();
    }

    /**
     * Les conditions se cumulent.
     *
     * @param subjectSlugs restreint à ces sujets (un ensemble vide ne laisse passer aucune carte), ou tous les sujets si
     *     {@code null}
     * @param themeSlug restreint à un thème, ou tous les thèmes si {@code null}
     * @param themeSelection restreint chaque sujet aux thèmes de cet ensemble qui lui appartiennent ; un sujet dont aucun
     *     thème n'y figure garde tous ses thèmes. Vide : aucune restriction.
     * @param types restreint à ces formats ; vide : tous les formats
     */
    record CardFilter(
            @Nullable Set<String> subjectSlugs,
            @Nullable String themeSlug,
            Set<String> themeSelection,
            Set<CardType> types) {

        public CardFilter {
            subjectSlugs = subjectSlugs == null ? null : Set.copyOf(subjectSlugs);
            themeSelection = Set.copyOf(themeSelection);
            types = Set.copyOf(types);
        }

        public static CardFilter all() {
            return new CardFilter(null, null, Set.of(), Set.of());
        }
    }

    /** Une carte active et son sujet, pour répartir les nouvelles cartes entre les sujets. */
    /** @param notionId la notion de la carte ; {@code null} pour une carte d'un document Markdown */
    record ActiveCard(
            UUID cardId, String subjectSlug, @Nullable UUID notionId) {

        /** Une carte sans notion. */
        public ActiveCard(UUID cardId, String subjectSlug) {
            this(cardId, subjectSlug, null);
        }
    }

    record ThemeCards(String subjectSlug, String subjectTitle, String slug, String title, List<UUID> cardIds) {

        public ThemeCards {
            cardIds = List.copyOf(cardIds);
        }
    }

    /**
     * Une carte dans sa version courante : {@code versionId} est l'identifiant de la version révisée.
     *
     * @param choices les choix d'un QCM, dans l'ordre de la source ; aucun pour les autres formats
     * @param blanks les trous d'un code à trous, dans l'ordre de la question ; aucun pour les autres formats
     * @param notionTitle le titre de la notion de la carte ; {@code null} pour une carte d'un document Markdown
     * @param lesson la leçon de cette notion, en Markdown ; {@code null} si elle n'en a pas encore
     */
    record CardSnapshot(
            UUID cardId,
            UUID versionId,
            CardType type,
            String subjectSlug,
            String subjectTitle,
            String themeSlug,
            String themeTitle,
            String prompt,
            String answer,
            List<CardChoice> choices,
            List<CardBlank> blanks,
            @Nullable ExerciseFiles exercise,
            @Nullable String notionTitle,
            @Nullable String lesson) {

        public CardSnapshot {
            choices = List.copyOf(choices);
            blanks = List.copyOf(blanks);
        }

        /**
         * Pour chaque trou, si la réponse donnée est acceptée.
         *
         * @throws IllegalArgumentException s'il n'y a pas une réponse par trou
         */
        public List<Boolean> checkBlanks(List<String> filled) {
            if (filled.size() != blanks.size()) {
                throw new IllegalArgumentException(
                        "%d réponse(s) pour %d trou(s)".formatted(filled.size(), blanks.size()));
            }
            return IntStream.range(0, blanks.size())
                    .mapToObj(index -> blanks.get(index).accepts(filled.get(index)))
                    .toList();
        }

        /** Les positions (à partir de 0) des bonnes réponses d'un QCM. */
        public Set<Integer> correctChoices() {
            return IntStream.range(0, choices.size())
                    .filter(index -> choices.get(index).correct())
                    .boxed()
                    .collect(Collectors.toUnmodifiableSet());
        }

        /** Une réponse à un QCM est juste si elle coche toutes les bonnes réponses, et elles seules. */
        public boolean isCorrectSelection(Set<Integer> selected) {
            return selected.equals(correctChoices());
        }
    }
}
