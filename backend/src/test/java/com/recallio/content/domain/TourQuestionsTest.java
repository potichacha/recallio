package com.recallio.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import java.util.List;
import org.junit.jupiter.api.Test;

class TourQuestionsTest {

    /** @param askable vrai si le serveur corrige la carte sans exécuter de code : des choix, ou des trous */
    private record Card(String key, CardType type, boolean askable) {}

    private static Card graded(String key, CardType type) {
        return new Card(key, type, true);
    }

    private static Card withoutChoices(String key, CardType type) {
        return new Card(key, type, false);
    }

    private static List<Card> pick(List<Card> cards) {
        return TourQuestions.pick(cards, Card::type, Card::askable);
    }

    @Test
    void shouldTakeOneCardOfEachFormatFirstInTheOrderOfTheFile() {
        List<Card> cards = List.of(
                withoutChoices("cours", CardType.FLASHCARD),
                graded("vf-1", CardType.TRUE_FALSE),
                graded("vf-2", CardType.TRUE_FALSE),
                graded("qcm-1", CardType.MULTIPLE_CHOICE),
                graded("trous-1", CardType.FILL_IN),
                withoutChoices("exercice", CardType.CODING_EXERCISE));

        assertThat(pick(cards)).extracting(Card::key).containsExactly("vf-1", "qcm-1", "trous-1");
    }

    @Test
    void shouldCompleteWithTheNextCardsWhenFewerFormatsAreAvailable() {
        List<Card> cards = List.of(
                graded("qcm-1", CardType.MULTIPLE_CHOICE),
                graded("vf-1", CardType.TRUE_FALSE),
                graded("qcm-2", CardType.MULTIPLE_CHOICE),
                graded("qcm-3", CardType.MULTIPLE_CHOICE));

        assertThat(pick(cards)).extracting(Card::key).containsExactly("qcm-1", "vf-1", "qcm-2");
    }

    /** Le critère porte sur la carte, plus sur son format (ADR 0063). */
    @Test
    void shouldAskACourseQuestionOrABugThatHasItsChoices() {
        List<Card> cards = List.of(
                graded("cours", CardType.FLASHCARD),
                graded("bug", CardType.BUG_FIX),
                graded("vf-1", CardType.TRUE_FALSE),
                graded("vf-2", CardType.TRUE_FALSE));

        assertThat(pick(cards)).extracting(Card::key).containsExactly("cours", "bug", "vf-1");
    }

    @Test
    void shouldKeepOnlyTheCardsGradedWithoutRunningCode() {
        List<Card> cards = List.of(
                withoutChoices("cours", CardType.FLASHCARD),
                withoutChoices("bug", CardType.BUG_FIX),
                graded("vf-1", CardType.TRUE_FALSE));

        assertThat(pick(cards)).extracting(Card::key).containsExactly("vf-1");
        assertThat(pick(List.of())).isEmpty();
    }
}
