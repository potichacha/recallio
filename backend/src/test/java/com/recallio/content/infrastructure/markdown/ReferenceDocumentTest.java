package com.recallio.content.infrastructure.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import com.recallio.content.domain.importing.CatalogSnapshot;
import com.recallio.content.domain.importing.ImportPlanner;
import com.recallio.content.domain.source.ParsedContent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Non-régression sur le vrai document de référence : si une modification du contenu casse le format, ce test le dit
 * avant l'import.
 */
class ReferenceDocumentTest {

    private static final Path REFERENCE = Path.of("..", "content", "source", "java-entretien-reference.md");
    private static final int CHAPTERS = 42;

    private static ParsedContent parsed;

    @BeforeAll
    static void parseReference() throws IOException {
        parsed = new MarkdownReferenceParser().parse(Files.readString(REFERENCE, StandardCharsets.UTF_8));
    }

    @Test
    void shouldFindEveryChapterAndTheGlossary() {
        assertThat(parsed.themes()).hasSize(CHAPTERS + 1);
    }

    @Test
    void shouldParseTheWholeDocumentWithoutIssues() {
        assertThat(parsed.issues()).isEmpty();
    }

    @Test
    void shouldGiveEveryQuestionItsOwnStableIdentifier() {
        var plan = new ImportPlanner().plan(parsed, CatalogSnapshot.empty());

        assertThat(plan.report().duplicates()).isEmpty();
        assertThat(plan.newCards()).hasSize(parsed.cardCount());
    }

    @Test
    void shouldProduceCardsOfEveryType() {
        Map<CardType, Long> countByType = parsed.themes().stream()
                .flatMap(theme -> theme.cards().stream())
                .collect(Collectors.groupingBy(card -> card.content().type(), Collectors.counting()));
        assertThat(countByType).containsOnlyKeys(CardType.values());
        assertThat(countByType.get(CardType.OPEN_SCENARIO)).isEqualTo(150);
        assertThat(countByType.get(CardType.DEFINITION)).isEqualTo(115);
    }

    @Test
    void shouldHaveCardsInEveryChapter() {
        assertThat(parsed.themes())
                .allSatisfy(theme -> assertThat(theme.cards()).isNotEmpty());
    }

    @Test
    void shouldHaveUniqueSlugs() {
        assertThat(parsed.themes().stream()
                        .collect(Collectors.groupingBy(theme -> theme.slug(), Collectors.counting())))
                .allSatisfy((slug, count) -> assertThat(count).as(slug).isEqualTo(1L));
    }
}
