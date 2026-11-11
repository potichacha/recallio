package com.recallio.content.domain.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.recallio.content.CardChoice;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.ContentQuality.Rule;
import com.recallio.content.domain.source.ContentQuality.Violation;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ContentQualityTest {

    private static final String LONG_ANSWER = "Dans le tas, depuis Java 7 ; avant, dans la PermGen";

    @Test
    void shouldFindNothingToSayAboutWellWrittenCards() {
        ContentQuality quality = quality(
                quiz("a", "Où se trouve le pool de chaînes ?", right("Dans le tas"), wrong("Dans la pile")),
                card(
                        "b",
                        new CardContent.TrueFalse(
                                "Une `String` est immuable.", "Chez Java, oui : assez tôt, tu le verras.", true)));

        assertThat(quality.subject()).isEqualTo("java");
        assertThat(quality.violations()).isEmpty();
    }

    @Test
    void shouldReportACorrectChoiceThatStandsOutByItsLength() {
        ContentQuality quality = quality(
                quiz("long", "Où se trouve le pool ?", right(LONG_ANSWER), wrong("Dans la pile"), wrong("Nulle part")),
                card("cours", new CardContent.Flashcard("Et avant ?", "…", List.of(right(LONG_ANSWER), wrong("Non")))));

        assertThat(quality.violations(Rule.LONGEST_CORRECT_CHOICE))
                .extracting(Violation::card)
                .containsExactly("notion/long", "notion/cours");
        assertThat(quality.violations(Rule.LONGEST_CORRECT_CHOICE).getFirst().detail())
                .isEqualTo("le bon choix fait 51 caractères, le plus long des mauvais 12");
    }

    @Test
    void shouldAcceptACorrectChoiceBarelyLongerOrAsLongAsAWrongOne() {
        ContentQuality quality = quality(
                // Deux fois plus long, mais de trois caractères : la longueur ne trahit rien.
                quiz("court", "Quel type pour un texte ?", right("String"), wrong("int")),
                quiz(
                        "voisin",
                        "Où se trouve le pool ?",
                        right(LONG_ANSWER),
                        wrong("Dans la pile de chaque thread, jamais")),
                // Plusieurs bonnes réponses : aucune ne se repère à sa longueur seule.
                quiz("plusieurs", "Lesquelles ?", right(LONG_ANSWER), right("Oui"), wrong("Non")),
                // Une sortie de programme a la longueur qu'elle a.
                quiz("code", "Qu'affiche ce code ?", right("```\n" + LONG_ANSWER + "\n```"), wrong("```\nA\n```")));

        assertThat(quality.count(Rule.LONGEST_CORRECT_CHOICE)).isZero();
    }

    @Test
    void shouldReportFormalAddressInTheQuestionTheChoicesOrTheExplanation() {
        ContentQuality quality = quality(
                card("question", new CardContent.Flashcard("Complétez la phrase.", "…")),
                quiz("choix", "Que fais-tu ?", right("Je lis votre message"), wrong("Rien")),
                card("explication", new CardContent.Flashcard("Pourquoi ?", "Parce que vous le pouvez.")));

        assertThat(quality.violations(Rule.FORMAL_ADDRESS))
                .extracting(Violation::card, Violation::detail)
                .containsExactly(
                        tuple("notion/question", "« Complétez »"),
                        tuple("notion/choix", "« votre »"),
                        tuple("notion/explication", "« vous »"));
    }

    @Test
    void shouldNotTakeCodeOrOtherWordsForFormalAddress() {
        ContentQuality quality = quality(
                card("code", new CardContent.Flashcard("Que fait `vos.lisez()` ?\n```java\nvar vous = 1;\n```", "…")),
                card("mots", new CardContent.Flashcard("Est-ce assez ?", "Chez Go, oui ; dévoué aussi.")));

        assertThat(quality.count(Rule.FORMAL_ADDRESS)).isZero();
    }

    @Test
    void shouldReportTheSameQuestionAskedTwiceInTheSameFormat() {
        ContentQuality quality = quality(
                card("a", new CardContent.Flashcard("Qu'est-ce qu'une goroutine ?", "…")),
                card("b", new CardContent.Flashcard("  qu'est-ce   qu'une GOROUTINE ?", "…")),
                // Un autre format, ou un autre code : une autre question.
                card("c", new CardContent.Definition("Qu'est-ce qu'une goroutine ?", "…")),
                card("d", new CardContent.CodeOutput("```go\nfmt.Println(1 + 2)\n```", "3")),
                card("e", new CardContent.CodeOutput("```go\nfmt.Println(1 - 2)\n```", "-1")));

        assertThat(quality.violations(Rule.DUPLICATE_QUESTION))
                .containsExactly(new Violation(Rule.DUPLICATE_QUESTION, "notion/b", "même question que notion/a"));
    }

    @Test
    void shouldReportTheQuizzesWithSeveralAnswersBeyondAQuarter() {
        List<ParsedCard> cards = new ArrayList<>();
        // Huit QCM, dont quatre à plusieurs réponses : deux sont permis.
        IntStream.range(0, 4)
                .forEach(i -> cards.add(quiz("une-" + i, "Laquelle, cas " + i + " ?", right("Oui"), wrong("Non"))));
        IntStream.range(0, 4)
                .forEach(i -> cards.add(
                        quiz("plusieurs-" + i, "Lesquelles, cas " + i + " ?", right("A"), right("B"), wrong("C"))));

        ContentQuality quality = quality(cards.toArray(ParsedCard[]::new));

        assertThat(quality.violations(Rule.MULTIPLE_ANSWERS))
                .extracting(Violation::card)
                .containsExactly("notion/plusieurs-2", "notion/plusieurs-3");
        assertThat(quality.violations(Rule.MULTIPLE_ANSWERS).getFirst().detail())
                .isEqualTo("4 QCM sur 8 ont plusieurs bonnes réponses, 2 au plus");
    }

    @Test
    void shouldNameACardOfADocumentByItsLine() {
        ContentQuality quality = quality(new ParsedCard(new CardContent.Flashcard("Complétez.", "…"), 42));

        assertThat(quality.violations()).extracting(Violation::card).containsExactly("ligne 42 (Complétez.)");
    }

    private static ContentQuality quality(ParsedCard... cards) {
        var subject = new Subject("java", "Java", Subject.Category.LANGUAGES, 10, Subject.Status.PUBLISHED);
        var theme = new ParsedTheme("langage", "java-langage", "Le langage", 1, List.of(cards));
        return ContentQuality.of(new ParsedContent(subject, List.of(theme), List.of()));
    }

    private static ParsedCard quiz(String key, String question, CardChoice... choices) {
        return card(key, new CardContent.MultipleChoice(question, "…", List.of(choices)));
    }

    private static ParsedCard card(String key, CardContent content) {
        return new ParsedCard(content, 0, new CardKey("notion", key));
    }

    private static CardChoice right(String text) {
        return new CardChoice(text, true);
    }

    private static CardChoice wrong(String text) {
        return new CardChoice(text, false);
    }
}
