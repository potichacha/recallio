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

    /** Le slug du sujet de chaque carte demandée, retirée ou non : de quoi compter un quota par sujet (ADR 0048). */
    Map<UUID, String> subjectsOf(Collection<UUID> cardIds);

    /** Les notions actives de ces sujets publiés, dans l'ordre de leur référentiel (ADR 0032). */
    List<NotionInfo> notions(Collection<String> subjectSlugs);

    /** La notion active de ce slug dans ce sujet publié ; vide si elle n'existe pas. */
    default Optional<NotionInfo> notion(String subjectSlug, String notionSlug) {
        return notions(Set.of(subjectSlug)).stream()
                .filter(notion -> notion.slug().equals(notionSlug))
                .findFirst();
    }

    /** Tous les thèmes publiés, dans l'ordre du programme, avec les identifiants de leurs cartes actives. */
    List<ThemeCards> activeCardsByTheme();

    /**
     * Les questions qui valident une notion du Tour (ADR 0037) : au plus trois de ses cartes que le serveur corrige sans
     * exécuter de code (des choix ou des trous, ADR 0063), dans l'ordre de son fichier. Vide pour une notion inconnue,
     * retirée, hors du Tour, ou d'un sujet non publié.
     */
    List<CardSnapshot> tourQuestions(String subjectSlug, String notionSlug);

    /**
     * Les cartes, retirées ou non, de celles de ces versions que le serveur corrige : des choix, des trous ou les tests
     * d'un exercice (ADR 0063). Une version sans choix, d'un document Markdown ou d'avant la migration (ADR 0051), n'en
     * est pas : sa réussite était un « Je savais ». Les versions inconnues sont ignorées.
     */
    Set<UUID> cardsOfVersionsGradedByServer(Collection<UUID> versionIds);

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
     * @param grading restreint aux cartes que le serveur sait corriger ainsi, d'après leur version courante
     */
    record CardFilter(
            @Nullable Set<String> subjectSlugs,
            @Nullable String themeSlug,
            Set<String> themeSelection,
            Set<CardType> types,
            Grading grading) {

        public CardFilter {
            subjectSlugs = subjectSlugs == null ? null : Set.copyOf(subjectSlugs);
            themeSelection = Set.copyOf(themeSelection);
            types = Set.copyOf(types);
        }

        /** Un filtre qui garde toutes les cartes, quelle que soit leur correction. */
        public CardFilter(
                @Nullable Set<String> subjectSlugs,
                @Nullable String themeSlug,
                Set<String> themeSelection,
                Set<CardType> types) {
            this(subjectSlugs, themeSlug, themeSelection, types, Grading.ANY);
        }

        public static CardFilter all() {
            return new CardFilter(null, null, Set.of(), Set.of());
        }

        /** Le même filtre, restreint aux cartes que le serveur corrige ainsi. */
        public CardFilter withGrading(Grading grading) {
            return new CardFilter(subjectSlugs, themeSlug, themeSelection, types, grading);
        }

        /**
         * Ce que le serveur doit savoir corriger d'une carte (ADR 0063). Le critère porte sur la carte, plus sur son
         * format : une question de cours qui a ses choix se corrige comme un QCM.
         */
        public enum Grading {
            /** Toutes les cartes. */
            ANY,
            /** Des choix : de quoi répondre d'un geste, sous un chrono court (défis de groupe, parties live). */
            CHOICES,
            /** Des choix, ou un code à trous : sans exécuter de code (duels, examen blanc, essai, questions du Tour). */
            CHOICES_OR_BLANKS,
            /** Des choix, un code à trous, ou un exercice jugé par ses tests : tout ce qui se révise. */
            SERVER
        }
    }

    /**
     * Une carte active, son sujet, sa notion et son format : de quoi répartir les nouvelles cartes et mesurer la
     * maîtrise (ADR 0034) sans charger le texte des cartes.
     *
     * @param notionId la notion de la carte ; {@code null} pour une carte d'un document Markdown
     */
    record ActiveCard(
            UUID cardId, String subjectSlug, @Nullable UUID notionId, CardType type) {}

    record ThemeCards(String subjectSlug, String subjectTitle, String slug, String title, List<UUID> cardIds) {

        public ThemeCards {
            cardIds = List.copyOf(cardIds);
        }
    }

    /**
     * Une carte dans sa version courante : {@code versionId} est l'identifiant de la version révisée.
     *
     * @param choices les choix de la carte, dans l'ordre de la source ; aucun pour un code à trous, un exercice, ou une
     *     carte d'un document Markdown qui n'en a pas
     * @param blanks les trous d'un code à trous, dans l'ordre de la question ; aucun pour les autres formats
     * @param notionSlug le slug de la notion de la carte, qui mène à sa leçon ; {@code null} pour une carte d'un
     *     document Markdown
     * @param notionTitle le titre de cette notion ; {@code null} de même
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
            @Nullable String notionSlug,
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

        /**
         * Vrai si la carte se corrige sur ses choix : un QCM, un vrai ou faux, ou une carte d'un autre format qui a les
         * siens (ADR 0051).
         */
        public boolean isGradedByChoices() {
            return !choices.isEmpty();
        }

        /**
         * Vrai si le serveur corrige la réponse sans exécuter de code : sur ses choix, ou sur ses trous. Le sens de
         * {@link CardFilter.Grading#CHOICES_OR_BLANKS}.
         */
        public boolean isGradedWithoutRunningCode() {
            return isGradedByChoices() || type == CardType.FILL_IN;
        }

        /**
         * Vrai si le serveur corrige la réponse : sur ses choix, ses trous, ou les tests d'un exercice (ADR 0035). Le
         * sens de {@link CardFilter.Grading#SERVER} ; sans cela, une carte ne se révise pas (ADR 0063).
         */
        public boolean isGradedByServer() {
            return isGradedByChoices() || type.isGradedByServer();
        }
    }
}
