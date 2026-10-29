package com.recallio.content.infrastructure.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedTheme;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MarkdownReferenceParserTest {

    private static ParsedContent parsed;

    @BeforeAll
    static void parseSample() throws IOException {
        try (InputStream input = Objects.requireNonNull(
                MarkdownReferenceParserTest.class.getResourceAsStream("/content/reference-sample.md"))) {
            parsed = new MarkdownReferenceParser().parse(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void shouldCreateOneThemePerChapterPlusTheGlossary() {
        assertThat(parsed.themes())
                .extracting(ParsedTheme::key, ParsedTheme::slug, ParsedTheme::position)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("01", "java-fondamentaux", 1),
                        org.assertj.core.groups.Tuple.tuple("14", "questions-pieges-qu-affiche-ce-code", 14),
                        org.assertj.core.groups.Tuple.tuple("glossary", "glossaire-des-sigles-et-termes", 1000));
    }

    @Test
    void shouldIgnoreCodeCommentsThatLookLikeTitles() {
        assertThat(parsed.themes()).noneMatch(theme -> theme.title().contains("commentaire"));
    }

    @Test
    void shouldIgnoreQuestionsOutsideQuestionSections() {
        assertThat(allPrompts())
                .noneMatch(prompt -> prompt.contains("fausse question") || prompt.contains("hors chapitre"));
    }

    @Nested
    class TechnicalQuestions {

        @Test
        void shouldParseInlineQuestionAndAnswerKeepingInnerMarkdown() {
            assertThat(cards("01").getFirst().content())
                    .isEqualTo(new CardContent.Flashcard(
                            "Qu'est-ce que la **JVM** ?", "La machine virtuelle qui exécute le bytecode."));
        }

        @Test
        void shouldProduceCodeOutputCardsInTheTrickQuestionsChapter() {
            assertThat(cards("14")).extracting(card -> card.content().type()).containsOnly(CardType.CODE_OUTPUT);
        }

        @Test
        void shouldReadCodeBlockQuestionsUpToTheAnswerLine() {
            CardContent codeQuestion = cards("14").get(1).content();
            assertThat(codeQuestion.prompt())
                    .startsWith("```java")
                    .contains("Integer a = 1000, b = 1000;")
                    .endsWith("```");
            assertThat(codeQuestion.answer()).isEqualTo("`false` : `==` compare les références hors du cache.");
        }
    }

    @Nested
    class SituationQuestions {

        @Test
        void shouldReadMultilineAnswersAndRemoveSurroundingQuotes() {
            CardContent scenario = cards("01").get(2).content();
            assertThat(scenario).isInstanceOf(CardContent.OpenScenario.class);
            assertThat(scenario.prompt())
                    .isEqualTo("Deux objets identiques sont différents dans une HashMap. Comment tu investigues ?");
            assertThat(scenario.answer())
                    .isEqualTo("Je vérifie `equals` et `hashCode`.\nEnsuite, je vérifie l'immutabilité des champs.");
        }

        @Test
        void shouldIgnoreAnnotationsAndKeepCodeBlocksInAnswers() {
            CardContent scenario = cards("01").get(3).content();
            assertThat(scenario.prompt()).isEqualTo("Comment tu optimises un projet Quarkus ?");
            assertThat(scenario.answer()).contains("```java", "Instant start = Instant.now();", "point chaud.");
        }
    }

    @Test
    void shouldTurnGlossaryRowsIntoDefinitionsWithoutTheHeader() {
        assertThat(cards("glossary"))
                .extracting(ParsedCard::content)
                .containsExactly(
                        new CardContent.Definition(
                                "ACID", "Atomicity, Consistency, Isolation, Durability : garanties des transactions"),
                        new CardContent.Definition("JVM", "Java Virtual Machine"));
    }

    @Test
    void shouldReportUnrecognizedLinesAndQuestionsWithoutAnswer() {
        assertThat(parsed.issues())
                .extracting(SourceIssue::reason)
                .containsExactly(
                        "Ligne non reconnue hors d'une question", "Question sans réponse (ligne « R : » attendue)");
        assertThat(parsed.issues().getFirst().excerpt()).isEqualTo("Une ligne orpheline qui ne correspond à rien.");
    }

    @Test
    void shouldRecordTheSourceLineOfEachQuestion() {
        assertThat(cards("01").getFirst().sourceLine()).isEqualTo(22);
    }

    private static List<ParsedCard> cards(String themeKey) {
        return parsed.themes().stream()
                .filter(theme -> theme.key().equals(themeKey))
                .findFirst()
                .orElseThrow()
                .cards();
    }

    private static List<String> allPrompts() {
        return parsed.themes().stream()
                .flatMap(theme -> theme.cards().stream())
                .map(card -> card.content().prompt())
                .toList();
    }
}
