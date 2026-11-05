package com.recallio.content.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import java.util.List;
import org.junit.jupiter.api.Test;

class TourQuestionsTest {

    private record Card(String key, CardType type) {}

    @Test
    void shouldTakeOneCardOfEachFormatFirstInTheOrderOfTheFile() {
        List<Card> cards = List.of(
                new Card("cours", CardType.FLASHCARD),
                new Card("vf-1", CardType.TRUE_FALSE),
                new Card("vf-2", CardType.TRUE_FALSE),
                new Card("qcm-1", CardType.MULTIPLE_CHOICE),
                new Card("trous-1", CardType.FILL_IN),
                new Card("exercice", CardType.CODING_EXERCISE));

        assertThat(TourQuestions.pick(cards, Card::type))
                .extracting(Card::key)
                .containsExactly("vf-1", "qcm-1", "trous-1");
    }

    @Test
    void shouldCompleteWithTheNextCardsWhenFewerFormatsAreAvailable() {
        List<Card> cards = List.of(
                new Card("qcm-1", CardType.MULTIPLE_CHOICE),
                new Card("vf-1", CardType.TRUE_FALSE),
                new Card("qcm-2", CardType.MULTIPLE_CHOICE),
                new Card("qcm-3", CardType.MULTIPLE_CHOICE));

        assertThat(TourQuestions.pick(cards, Card::type))
                .extracting(Card::key)
                .containsExactly("qcm-1", "vf-1", "qcm-2");
    }

    @Test
    void shouldKeepOnlyTheCardsGradedByTheServer() {
        List<Card> cards = List.of(
                new Card("cours", CardType.FLASHCARD),
                new Card("bug", CardType.BUG_FIX),
                new Card("vf-1", CardType.TRUE_FALSE));

        assertThat(TourQuestions.pick(cards, Card::type)).extracting(Card::key).containsExactly("vf-1");
        assertThat(TourQuestions.pick(List.<Card>of(), Card::type)).isEmpty();
    }
}
