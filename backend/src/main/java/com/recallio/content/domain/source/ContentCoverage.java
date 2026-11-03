package com.recallio.content.domain.source;

import com.recallio.content.CardType;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * La couverture du référentiel d'un sujet par ses cartes (ADR 0025) : pour chaque notion, le nombre de cartes de chaque
 * format qu'elle retient. Une notion est <em>complète</em> quand chacun de ses formats atteint la cible de la matrice.
 *
 * <p>Une mesure, pas une règle : la CI la publie sans échouer, jusqu'à ce que le pilote fixe le niveau d'exigence.
 *
 * @param subject le slug du sujet
 * @param notions les notions dans l'ordre du référentiel
 */
public record ContentCoverage(String subject, List<NotionCoverage> notions) {

    /** Cartes visées par format et par notion : le bas de la fourchette de la matrice (4 à 5, ADR 0025). */
    public static final int TARGET_PER_FORMAT = 4;

    public ContentCoverage {
        notions = List.copyOf(notions);
    }

    /**
     * Une notion du référentiel et les formats qu'il retient pour elle, parmi ceux que l'application sait présenter
     * (les exercices de code, comptés par chapitre, n'en font pas partie).
     *
     * @param declaredComplete vrai si le référentiel déclare la notion complète : elle doit alors le rester
     */
    public record NotionTarget(ParsedNotion notion, Set<CardType> formats, boolean declaredComplete) {

        public NotionTarget {
            formats = Set.copyOf(formats);
        }

        /** Une notion que le référentiel ne déclare pas complète. */
        public NotionTarget(ParsedNotion notion, Set<CardType> formats) {
            this(notion, formats, false);
        }
    }

    public static ContentCoverage of(String subject, List<NotionTarget> targets, List<ParsedCard> cards) {
        Map<String, Map<CardType, Integer>> counts = new HashMap<>();
        for (ParsedCard card : cards) {
            CardKey key = card.key();
            if (key != null) {
                counts.computeIfAbsent(key.notion(), _ -> new EnumMap<>(CardType.class))
                        .merge(card.content().type(), 1, Integer::sum);
            }
        }
        return new ContentCoverage(
                subject,
                targets.stream()
                        .map(target -> new NotionCoverage(
                                target.notion(),
                                target.formats(),
                                counts.getOrDefault(target.notion().slug(), Map.of()),
                                target.declaredComplete()))
                        .toList());
    }

    /** La couverture de chaque priorité présente dans le référentiel, de la plus essentielle à la plus rare. */
    public List<PriorityCoverage> byPriority() {
        Map<Integer, PriorityCoverage> byPriority = new TreeMap<>();
        for (NotionCoverage notion : notions) {
            byPriority.merge(notion.notion().priority(), PriorityCoverage.of(notion), PriorityCoverage::plus);
        }
        return List.copyOf(byPriority.values());
    }

    /** Les notions de cette priorité qui n'ont encore aucune carte. */
    public List<NotionCoverage> uncovered(int priority) {
        return notions.stream()
                .filter(notion -> notion.notion().priority() == priority && !notion.covered())
                .toList();
    }

    /** Les exercices de code, comptés par chapitre : la matrice en vise au moins un par chapitre (ADR 0025). */
    public ChapterExercises chapterExercises() {
        Map<String, Integer> byChapter = new TreeMap<>();
        for (NotionCoverage notion : notions) {
            byChapter.merge(
                    notion.notion().domain() + "/" + notion.notion().chapter(),
                    notion.count(CardType.CODING_EXERCISE),
                    Integer::sum);
        }
        return new ChapterExercises(
                byChapter.size(),
                (int) byChapter.values().stream().filter(count -> count > 0).count(),
                byChapter.values().stream().mapToInt(Integer::intValue).sum());
    }

    /**
     * Ce qui manque aux notions déclarées complètes (ADR 0025), pour que la CI échoue si l'une d'elles régresse :
     * chacun de ses formats à la cible, sa leçon, et un exercice dans un chapitre dont toutes les notions essentielles
     * sont déclarées complètes. Vide si tout est en ordre.
     */
    public List<String> completionProblems() {
        List<String> problems = new ArrayList<>();
        for (NotionCoverage notion : notions) {
            if (!notion.declaredComplete()) {
                continue;
            }
            String slug = notion.notion().slug();
            notion.formats().stream()
                    .filter(format -> notion.count(format) < TARGET_PER_FORMAT)
                    .sorted()
                    .forEach(format -> problems.add("%s : %d carte(s) %s sur %d visées"
                            .formatted(slug, notion.count(format), format, TARGET_PER_FORMAT)));
            if (notion.notion().lesson() == null) {
                problems.add(slug + " : pas de leçon");
            }
        }
        chaptersCompleteWithoutExercise().forEach(chapter -> problems.add(chapter + " : pas d'exercice de code"));
        return problems;
    }

    /** Les chapitres dont toutes les notions essentielles sont déclarées complètes, mais sans exercice de code. */
    private List<String> chaptersCompleteWithoutExercise() {
        Map<String, List<NotionCoverage>> byChapter = new TreeMap<>();
        notions.forEach(notion -> byChapter
                .computeIfAbsent(
                        notion.notion().domain() + "/" + notion.notion().chapter(), _ -> new ArrayList<>())
                .add(notion));
        return byChapter.entrySet().stream()
                .filter(chapter -> {
                    List<NotionCoverage> essential = chapter.getValue().stream()
                            .filter(notion -> notion.notion().priority() == 1)
                            .toList();
                    return !essential.isEmpty()
                            && essential.stream().allMatch(NotionCoverage::declaredComplete)
                            && chapter.getValue().stream()
                                    .allMatch(notion -> notion.count(CardType.CODING_EXERCISE) == 0);
                })
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * @param chapters les chapitres du référentiel
     * @param chaptersWithExercise ceux qui ont au moins un exercice de code
     * @param exercises les exercices de code du sujet
     */
    public record ChapterExercises(int chapters, int chaptersWithExercise, int exercises) {}

    /**
     * @param cards le nombre de cartes par format ; un format absent n'a aucune carte
     * @param declaredComplete vrai si le référentiel la déclare complète
     */
    public record NotionCoverage(
            ParsedNotion notion, Set<CardType> formats, Map<CardType, Integer> cards, boolean declaredComplete) {

        public NotionCoverage {
            formats = Set.copyOf(formats);
            cards = Map.copyOf(cards);
        }

        public int count(CardType format) {
            return cards.getOrDefault(format, 0);
        }

        public boolean covered() {
            return !cards.isEmpty();
        }

        /** Chaque format retenu atteint la cible ; une notion sans format retenu n'est jamais complète. */
        public boolean complete() {
            return !formats.isEmpty() && formats.stream().allMatch(format -> count(format) >= TARGET_PER_FORMAT);
        }
    }

    /**
     * @param notions les notions de la priorité
     * @param covered celles qui ont au moins une carte
     * @param complete celles dont tous les formats atteignent la cible
     */
    public record PriorityCoverage(int priority, int notions, int covered, int complete) {

        static PriorityCoverage of(NotionCoverage notion) {
            return new PriorityCoverage(
                    notion.notion().priority(), 1, notion.covered() ? 1 : 0, notion.complete() ? 1 : 0);
        }

        PriorityCoverage plus(PriorityCoverage other) {
            return new PriorityCoverage(
                    priority, notions + other.notions, covered + other.covered, complete + other.complete);
        }
    }
}
