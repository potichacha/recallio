package com.recallio.content.infrastructure.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.Subject;
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
                .extracting(ParsedTheme::key, ParsedTheme::title, ParsedTheme::position)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("01", "JAVA : FONDAMENTAUX", 1),
                        org.assertj.core.groups.Tuple.tuple("14", "QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?", 14),
                        org.assertj.core.groups.Tuple.tuple("glossary", "GLOSSAIRE DES SIGLES ET TERMES", 1000));
    }

    @Test
    void shouldReadTheSubjectFromTheHeader() {
        assertThat(parsed.subject())
                .isEqualTo(new Subject("java", "Java", Subject.Category.LANGUAGES, 10, Subject.Status.PUBLISHED));
    }

    @Test
    void shouldRecognizeTheTrickQuestionsChapterByItsTitleWhateverItsNumber() {
        ParsedContent other = new MarkdownReferenceParser().parse("""
                ---
                subject: go
                title: Go
                category: languages
                position: 20
                status: draft
                ---
                # 3. QUESTIONS PIÈGES : QU'AFFICHE CE CODE ?
                ### Questions techniques
                **Q : `fmt.Println(len("é"))`** R : `2` : len compte les octets.
                """);

        assertThat(other.themes().getFirst().cards())
                .extracting(card -> card.content().type())
                .containsExactly(CardType.CODE_OUTPUT);
        assertThat(other.subject().status()).isEqualTo(Subject.Status.DRAFT);
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
        assertThat(cards("01").getFirst().sourceLine()).isEqualTo(30);
    }

    @Nested
    class MultipleChoiceSection {

        private static final String DOCUMENT = """
                ---
                subject: go
                title: Go
                category: languages
                position: 20
                status: published
                ---
                # 1. GO : CONCURRENCE

                ### QCM

                **Q : Que produit la lecture d'un channel fermé ?**
                - [ ] Un blocage
                - [x] La valeur zéro du type
                - [ ] Une panique
                R : Elle ne bloque jamais.
                Avec `v, ok := <-ch`, `ok` vaut `false`.

                **Q : Quels mots-clés lancent ou synchronisent du code concurrent ?**
                - [X] `go`
                - [x] `select`
                - [ ] `defer`
                R : `go` lance une goroutine, `select` attend plusieurs channels.

                **Q : Un QCM sans bonne réponse ?**
                - [ ] Non
                - [ ] Toujours non
                R : Refusé.

                **Q : Un seul choix ?**
                - [x] Oui
                R : Refusé aussi.
                """;

        private final ParsedContent content = new MarkdownReferenceParser().parse(DOCUMENT);

        @Test
        void shouldReadTheChoicesAndAMultilineExplanation() {
            var card = (CardContent.MultipleChoice)
                    content.themes().getFirst().cards().getFirst().content();

            assertThat(card.type()).isEqualTo(CardType.MULTIPLE_CHOICE);
            assertThat(card.prompt()).isEqualTo("Que produit la lecture d'un channel fermé ?");
            assertThat(card.choices())
                    .containsExactly(
                            new CardChoice("Un blocage", false),
                            new CardChoice("La valeur zéro du type", true),
                            new CardChoice("Une panique", false));
            assertThat(card.answer()).isEqualTo("Elle ne bloque jamais.\nAvec `v, ok := <-ch`, `ok` vaut `false`.");
        }

        @Test
        void shouldAcceptSeveralCorrectChoices() {
            var card = content.themes().getFirst().cards().get(1).content();

            assertThat(card.choices()).extracting(CardChoice::correct).containsExactly(true, true, false);
        }

        @Test
        void shouldReportAQuestionWithoutACorrectChoiceOrWithASingleChoice() {
            assertThat(content.themes().getFirst().cards()).hasSize(2);
            assertThat(content.issues()).extracting(SourceIssue::line).containsExactly(25, 30);
            assertThat(content.issues().getFirst().reason()).startsWith("QCM : de 2 à 8 choix");
        }
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
