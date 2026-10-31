package com.recallio.content.infrastructure.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import com.recallio.content.domain.CardIdentity;
import com.recallio.content.domain.importing.CatalogSnapshot;
import com.recallio.content.domain.importing.ImportPlanner;
import com.recallio.content.domain.source.ParsedContent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contrôle de tous les documents de référence de {@code content/source} (CLAUDE.md, section 5.2) : si un document est
 * mal formé, ce test le dit dans la CI, avant le merge et avant l'import. Un nouveau sujet est vérifié dès qu'il est
 * ajouté au dossier, sans modifier ce test.
 */
class ReferenceDocumentTest {

    private static final Path SOURCES = Path.of("..", "content", "source");
    private static final String SUFFIX = "-entretien-reference.md";

    static Stream<Path> documents() throws IOException {
        try (Stream<Path> files = Files.list(SOURCES)) {
            List<Path> documents = files.filter(
                            file -> file.getFileName().toString().endsWith(SUFFIX))
                    .sorted()
                    .toList();
            assertThat(documents).as("documents de référence").isNotEmpty();
            return documents.stream();
        }
    }

    @ParameterizedTest
    @MethodSource("documents")
    void shouldBeNamedAfterItsSubject(Path document) {
        String fileName = document.getFileName().toString();

        assertThat(parse(document).subject().slug() + SUFFIX).isEqualTo(fileName);
    }

    @ParameterizedTest
    @MethodSource("documents")
    void shouldParseWithoutIssues(Path document) {
        assertThat(parse(document).issues()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("documents")
    void shouldGiveEveryQuestionItsOwnStableIdentifier(Path document) {
        ParsedContent parsed = parse(document);
        var plan = new ImportPlanner().plan(parsed, CatalogSnapshot.empty());

        assertThat(plan.report().duplicates()).isEmpty();
        assertThat(plan.newCards()).hasSize(parsed.cardCount());
    }

    @ParameterizedTest
    @MethodSource("documents")
    void shouldHaveCardsInEveryChapter(Path document) {
        assertThat(parse(document).themes())
                .isNotEmpty()
                .allSatisfy(theme -> assertThat(theme.cards()).as(theme.title()).isNotEmpty());
    }

    @ParameterizedTest
    @MethodSource("documents")
    void shouldProduceCardsOfEveryType(Path document) {
        Map<CardType, Long> countByType = parse(document).themes().stream()
                .flatMap(theme -> theme.cards().stream())
                .collect(Collectors.groupingBy(card -> card.content().type(), Collectors.counting()));

        assertThat(countByType).containsOnlyKeys(CardType.values());
    }

    @Test
    void shouldHaveUniqueSubjectsAndThemeSlugsAcrossAllDocuments() throws IOException {
        List<ParsedContent> all = documents().map(ReferenceDocumentTest::parse).toList();

        assertThat(all).extracting(parsed -> parsed.subject().slug()).doesNotHaveDuplicates();
        assertThat(all.stream()
                        .flatMap(parsed -> parsed.themes().stream()
                                .map(theme ->
                                        CardIdentity.themeSlug(parsed.subject().slug(), theme.title())))
                        .toList())
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldKeepTheJavaDocumentComplete() {
        ParsedContent java = parse(SOURCES.resolve("java" + SUFFIX));

        assertThat(java.themes()).hasSize(42 + 1);
        assertThat(java.cardCount()).isEqualTo(1311);
    }

    private static ParsedContent parse(Path document) {
        try {
            return new MarkdownReferenceParser().parse(Files.readString(document, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
