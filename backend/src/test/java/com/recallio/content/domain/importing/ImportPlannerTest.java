package com.recallio.content.domain.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.CardIdentity;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.importing.CatalogSnapshot.ExistingCard;
import com.recallio.content.domain.importing.CatalogSnapshot.ExistingTheme;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedTheme;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ImportPlannerTest {

    private static final Subject JAVA =
            new Subject("java", "Java", Subject.Category.LANGUAGES, 10, Subject.Status.PUBLISHED);
    private static final String THEME_KEY = "01";
    private static final UUID THEME_ID = CardIdentity.themeId(JAVA.slug(), THEME_KEY);
    private static final CardContent JVM =
            new CardContent.Flashcard("Qu'est-ce que la JVM ?", "Une machine virtuelle.");
    private static final CardContent JIT = new CardContent.Flashcard("Qu'est-ce que le JIT ?", "Un compilateur.");

    private final ImportPlanner planner = new ImportPlanner();

    @Test
    void shouldAddEverythingOnTheFirstImport() {
        ImportPlan plan = planner.plan(source(JVM, JIT), CatalogSnapshot.empty());

        assertThat(plan.report().themesCreated()).isEqualTo(1);
        assertThat(plan.newCards())
                .extracting(ImportPlan.NewCard::position, ImportPlan.NewCard::content)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(0, JVM), org.assertj.core.groups.Tuple.tuple(1, JIT));
    }

    @Test
    void shouldCreateTheSubjectOnItsFirstImport() {
        ImportPlan plan = planner.plan(source(JVM), CatalogSnapshot.empty());

        ImportPlan.SubjectUpsert upsert = Objects.requireNonNull(plan.subjectUpsert());
        assertThat(upsert.id()).isEqualTo(CardIdentity.subjectId("java"));
        assertThat(plan.themes()).singleElement().satisfies(theme -> {
            assertThat(theme.subjectId()).isEqualTo(CardIdentity.subjectId("java"));
            assertThat(theme.slug()).isEqualTo("java-fondamentaux");
        });
        assertThat(plan.report().subject()).isEqualTo("java");
    }

    @Test
    void shouldUpdateTheSubjectWhenItsHeaderChanges() {
        var published = new ParsedContent(
                new Subject("java", "Java 25", Subject.Category.LANGUAGES, 10, Subject.Status.PUBLISHED),
                source(JVM).themes(),
                List.of());

        ImportPlan plan = planner.plan(published, catalogOf(card(JVM, 0, 1, false)));

        assertThat(plan.subjectUpsert()).isNotNull();
        assertThat(plan.isEmpty()).isFalse();
    }

    @Test
    void shouldPlanNothingWhenTheSourceIsUnchanged() {
        ImportPlan plan = planner.plan(source(JVM, JIT), catalogOf(card(JVM, 0, 1, false), card(JIT, 1, 1, false)));

        assertThat(plan.isEmpty()).isTrue();
        assertThat(plan.report().cardsUnchanged()).isEqualTo(2);
    }

    @Test
    void shouldCreateANewVersionKeepingTheIdentifierWhenTheAnswerChanges() {
        var corrected = new CardContent.Flashcard(JVM.prompt(), "Une machine virtuelle qui exécute le bytecode.");

        ImportPlan plan = planner.plan(source(corrected), catalogOf(card(JVM, 0, 3, false)));

        assertThat(plan.newCards()).isEmpty();
        assertThat(plan.newVersions()).singleElement().satisfies(version -> {
            assertThat(version.cardId()).isEqualTo(idOf(JVM));
            assertThat(version.version()).isEqualTo(4);
            assertThat(version.content()).isEqualTo(corrected);
        });
    }

    @Test
    void shouldRetireCardsThatDisappearedFromTheSource() {
        ImportPlan plan = planner.plan(source(JIT), catalogOf(card(JVM, 0, 1, false), card(JIT, 1, 1, false)));

        assertThat(plan.retiredCardIds()).containsExactly(idOf(JVM));
        assertThat(plan.placements())
                .singleElement()
                .satisfies(placement -> assertThat(placement.position()).isZero());
    }

    @Test
    void shouldReactivateARetiredCardThatComesBack() {
        ImportPlan plan = planner.plan(source(JVM), catalogOf(card(JVM, 0, 1, true)));

        assertThat(plan.reactivatedCardIds()).containsExactly(idOf(JVM));
        assertThat(plan.retiredCardIds()).isEmpty();
    }

    @Test
    void shouldNotRetireAnAlreadyRetiredCardTwice() {
        ImportPlan plan = planner.plan(source(JIT), catalogOf(card(JVM, 0, 1, true), card(JIT, 0, 1, false)));

        assertThat(plan.retiredCardIds()).isEmpty();
    }

    @Test
    void shouldReportDuplicateQuestionsAndKeepOnlyTheFirst() {
        var duplicate = new CardContent.Flashcard("Qu'est-ce que la **JVM** ?", "Autre réponse.");

        ImportPlan plan = planner.plan(source(JVM, duplicate), CatalogSnapshot.empty());

        assertThat(plan.newCards()).hasSize(1);
        assertThat(plan.report().duplicates())
                .singleElement()
                .satisfies(found -> assertThat(found.line()).isEqualTo(2));
    }

    private static ParsedContent source(CardContent... contents) {
        List<ParsedCard> cards = Arrays.stream(contents)
                .map(content -> new ParsedCard(content, Arrays.asList(contents).indexOf(content) + 1))
                .toList();
        var theme = new ParsedTheme(THEME_KEY, "JAVA : FONDAMENTAUX", 1, cards);
        return new ParsedContent(JAVA, List.of(theme), List.of());
    }

    private static CatalogSnapshot catalogOf(ExistingCard... cards) {
        var theme = new ExistingTheme(THEME_ID, "java-fondamentaux", "JAVA : FONDAMENTAUX", 1);
        Map<UUID, ExistingCard> byId =
                Arrays.stream(cards).collect(Collectors.toMap(ExistingCard::id, Function.identity()));
        return new CatalogSnapshot(JAVA, Map.of(THEME_ID, theme), byId);
    }

    private static ExistingCard card(CardContent content, int position, int version, boolean retired) {
        return new ExistingCard(
                idOf(content), THEME_ID, content.type(), position, version, CardIdentity.contentHash(content), retired);
    }

    private static UUID idOf(CardContent content) {
        return CardIdentity.cardId(JAVA.slug(), THEME_KEY, content.prompt());
    }
}
