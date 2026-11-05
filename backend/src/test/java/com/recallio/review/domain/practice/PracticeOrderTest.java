package com.recallio.review.domain.practice;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import java.util.List;
import org.junit.jupiter.api.Test;

class PracticeOrderTest {

    private record Card(String key, CardType type) {}

    @Test
    void shouldAlternateTheFormatsInTheOrderOfTheFileAndPutTheExercisesLast() {
        List<Card> cards = List.of(
                new Card("exercice", CardType.CODING_EXERCISE),
                new Card("cours-1", CardType.FLASHCARD),
                new Card("cours-2", CardType.FLASHCARD),
                new Card("vf-1", CardType.TRUE_FALSE),
                new Card("vf-2", CardType.TRUE_FALSE),
                new Card("vf-3", CardType.TRUE_FALSE),
                new Card("qcm-1", CardType.MULTIPLE_CHOICE));

        assertThat(PracticeOrder.arrange(cards, Card::type))
                .extracting(Card::key)
                .containsExactly("cours-1", "vf-1", "qcm-1", "cours-2", "vf-2", "vf-3", "exercice");
    }

    @Test
    void shouldKeepEveryCardOnce() {
        assertThat(PracticeOrder.arrange(List.<Card>of(), Card::type)).isEmpty();
        assertThat(PracticeOrder.arrange(List.of(new Card("seul", CardType.DEFINITION)), Card::type))
                .extracting(Card::key)
                .containsExactly("seul");
    }
}
