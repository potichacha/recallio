package com.recallio.content.domain.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.NotionLevel;
import com.recallio.content.domain.source.ContentCoverage.NotionTarget;
import com.recallio.content.domain.source.ContentCoverage.PriorityCoverage;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ContentCoverageTest {

    private static final NotionTarget HASHSET = target("hashset", 1, Set.of(CardType.FLASHCARD, CardType.CODE_OUTPUT));
    private static final NotionTarget TREESET = target("treeset", 1, Set.of(CardType.FLASHCARD));
    private static final NotionTarget SPLITERATOR = target("spliterator", 3, Set.of(CardType.FLASHCARD));

    @Test
    void shouldCountTheCardsOfEachFormatPerNotion() {
        ContentCoverage coverage = ContentCoverage.of(
                "java",
                List.of(HASHSET),
                List.of(flashcard("hashset", "a"), flashcard("hashset", "b"), codeOutput("hashset", "c")));

        var hashset = coverage.notions().getFirst();
        assertThat(hashset.count(CardType.FLASHCARD)).isEqualTo(2);
        assertThat(hashset.count(CardType.CODE_OUTPUT)).isEqualTo(1);
        assertThat(hashset.count(CardType.MULTIPLE_CHOICE)).isZero();
        assertThat(hashset.covered()).isTrue();
        assertThat(hashset.complete()).isFalse();
    }

    @Test
    void shouldCallANotionCompleteOnlyWhenEveryRetainedFormatReachesTheTarget() {
        List<ParsedCard> cards = new ArrayList<>(atTarget("hashset", CardType.FLASHCARD));
        cards.addAll(atTarget("treeset", CardType.FLASHCARD));
        cards.addAll(atTarget("hashset", CardType.CODE_OUTPUT).subList(1, ContentCoverage.TARGET_PER_FORMAT));

        ContentCoverage coverage = ContentCoverage.of("java", List.of(HASHSET, TREESET), cards);

        assertThat(coverage.notions())
                .extracting(notion -> notion.notion().slug(), ContentCoverage.NotionCoverage::complete)
                .containsExactly(tuple("hashset", false), tuple("treeset", true));
    }

    @Test
    void shouldSummarizeEachPriorityAndListTheNotionsWithoutCards() {
        ContentCoverage coverage = ContentCoverage.of(
                "java", List.of(HASHSET, TREESET, SPLITERATOR), atTarget("treeset", CardType.FLASHCARD));

        assertThat(coverage.byPriority())
                .containsExactly(new PriorityCoverage(1, 2, 1, 1), new PriorityCoverage(3, 1, 0, 0));
        assertThat(coverage.uncovered(1))
                .extracting(notion -> notion.notion().slug())
                .containsExactly("hashset");
    }

    @Test
    void shouldIgnoreCardsWithoutKeyAndCardsOfAnotherNotion() {
        ContentCoverage coverage = ContentCoverage.of(
                "java",
                List.of(TREESET),
                List.of(
                        new ParsedCard(new CardContent.Flashcard("Sans clé ?", "Markdown."), 3),
                        flashcard("hashset", "a")));

        assertThat(coverage.notions().getFirst().covered()).isFalse();
    }

    @Test
    void shouldCountTheExercisesOfEachChapter() {
        var exercise = new ParsedCard(
                new CardContent.CodingExercise(
                        "Écrire un ensemble.",
                        "Avec un HashSet.",
                        new ExerciseFiles(
                                List.of(new SourceFile("Ensemble.java", "class Ensemble {}")),
                                List.of(new SourceFile("Ensemble.java", "class Ensemble { }")),
                                List.of(new SourceFile("EnsembleTest.java", "class EnsembleTest {}")))),
                0,
                new CardKey("hashset", "ensemble"));
        NotionTarget otherChapter = new NotionTarget(
                new ParsedNotion(
                        "list",
                        "collections",
                        "lists",
                        "Les listes",
                        "list",
                        NotionLevel.BEGINNER,
                        1,
                        List.of(),
                        1,
                        null),
                Set.of(CardType.FLASHCARD));

        ContentCoverage coverage =
                ContentCoverage.of("java", List.of(HASHSET, TREESET, otherChapter), List.of(exercise));

        assertThat(coverage.chapterExercises()).isEqualTo(new ContentCoverage.ChapterExercises(2, 1, 1));
        assertThat(coverage.notions().getFirst().complete()).isFalse();
    }

    private static List<ParsedCard> atTarget(String notion, CardType type) {
        return IntStream.range(0, ContentCoverage.TARGET_PER_FORMAT)
                .mapToObj(index -> type == CardType.CODE_OUTPUT
                        ? codeOutput(notion, type + "-" + index)
                        : flashcard(notion, type + "-" + index))
                .toList();
    }

    private static ParsedCard flashcard(String notion, String key) {
        return new ParsedCard(new CardContent.Flashcard("Q " + key, "R"), 0, new CardKey(notion, key));
    }

    private static ParsedCard codeOutput(String notion, String key) {
        return new ParsedCard(new CardContent.CodeOutput("Q " + key, "R"), 0, new CardKey(notion, key));
    }

    private static NotionTarget target(String slug, int priority, Set<CardType> formats) {
        return new NotionTarget(
                new ParsedNotion(
                        slug,
                        "collections",
                        "sets",
                        "Les ensembles",
                        slug,
                        NotionLevel.BEGINNER,
                        priority,
                        List.of(),
                        0,
                        null),
                formats);
    }
}
