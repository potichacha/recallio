package com.recallio.review.domain.track;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.ReviewState;
import com.recallio.review.domain.track.NotionMastery.NextLevel;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotionMasteryTest {

    private static final Instant NOW = Instant.parse("2026-11-04T09:00:00Z");

    /** Une notion de 8 cartes, en quatre formats : 3 de cours, 2 vrai ou faux, 2 QCM et un exercice de code. */
    private final Map<UUID, CardType> cards = cards(
            CardType.FLASHCARD,
            CardType.FLASHCARD,
            CardType.FLASHCARD,
            CardType.TRUE_FALSE,
            CardType.TRUE_FALSE,
            CardType.MULTIPLE_CHOICE,
            CardType.MULTIPLE_CHOICE,
            CardType.CODING_EXERCISE);

    private final List<UUID> ids = List.copyOf(cards.keySet());
    private final Map<UUID, ReviewState> states = new HashMap<>();
    private final Set<UUID> recalled = new HashSet<>();

    @Test
    void shouldBeNewUntilACardIsSeen() {
        NotionMastery mastery = mastery();

        assertThat(mastery.level()).isEqualTo(MasteryLevel.NEW);
        assertThat(mastery.nextLevel()).contains(new NextLevel(MasteryLevel.DISCOVERED, 0, 1, 0, 0, Set.of()));
    }

    @Test
    void shouldBeDiscoveredOnceACardIsSeenThenAcquiredAfterFiveRecalledCardsLikeThePath() {
        learning(0, 1, 2, 3, 4);
        recall(0, 1, 2, 3);

        assertThat(mastery().level()).isEqualTo(MasteryLevel.DISCOVERED);
        assertThat(mastery().nextLevel()).contains(new NextLevel(MasteryLevel.ACQUIRED, 4, 5, 0, 0, Set.of()));

        recall(4);
        assertThat(mastery().level()).isEqualTo(MasteryLevel.ACQUIRED);
        assertThat(LearningPath.isAcquired(mastery().recalled(), mastery().cardCount()))
                .isTrue();
    }

    /** Des réussites retirées depuis du programme comptent encore, comme dans le parcours. */
    @Test
    void shouldCountRecalledCardsRetiredSinceToAcquireTheNotion() {
        learning(0, 1, 2, 3);
        recall(0, 1, 2, 3);
        recalled.add(UUID.randomUUID());

        assertThat(mastery().level()).isEqualTo(MasteryLevel.ACQUIRED);
    }

    @Test
    void shouldNeedFiveStableCardsInThreeFormatsToMasterTheNotion() {
        stable(30, 0, 1, 2, 3, 4);
        recall(0, 1, 2, 3, 4);

        assertThat(mastery().level()).isEqualTo(MasteryLevel.ACQUIRED);
        assertThat(mastery().nextLevel()).contains(new NextLevel(MasteryLevel.MASTERED, 5, 5, 2, 3, Set.of()));

        stable(30, 5);
        recall(5);
        assertThat(mastery().level()).isEqualTo(MasteryLevel.MASTERED);
    }

    @Test
    void shouldLoseMasteryOnceACardIsForgotten() {
        stable(30, 0, 1, 2, 3, 5);
        recall(0, 1, 2, 3, 5);
        assertThat(mastery().level()).isEqualTo(MasteryLevel.MASTERED);

        // Notée « À revoir », la carte repasse en réapprentissage avec une stabilité faible.
        states.put(ids.get(5), new ReviewState(LearningState.RELEARNING, 0, 2, 6, NOW, 4, 1, NOW));

        assertThat(mastery().level()).isEqualTo(MasteryLevel.ACQUIRED);
    }

    @Test
    void shouldBecomeExpertWithLastingCardsAndEveryFormatOfTheNotionSucceeded() {
        stable(120, 0, 1, 2, 3, 5);
        recall(0, 1, 2, 3, 5);

        assertThat(mastery().level()).isEqualTo(MasteryLevel.MASTERED);
        assertThat(mastery().nextLevel())
                .contains(new NextLevel(MasteryLevel.EXPERT, 5, 5, 3, 3, Set.of(CardType.CODING_EXERCISE)));

        learning(7);
        recall(7);
        assertThat(mastery().level()).isEqualTo(MasteryLevel.EXPERT);
        assertThat(mastery().nextLevel()).isEmpty();
    }

    @Test
    void shouldStayMasteredWhileTheCardsAreNotRetainedForMonths() {
        stable(30, 0, 1, 2, 3, 5);
        learning(7);
        recall(0, 1, 2, 3, 5, 7);

        assertThat(mastery().level()).isEqualTo(MasteryLevel.MASTERED);
        assertThat(mastery().nextLevel()).contains(new NextLevel(MasteryLevel.EXPERT, 0, 5, 0, 3, Set.of()));
    }

    /** Une notion de 3 cartes, en 2 formats, demande toutes ses cartes et tous ses formats. */
    @Test
    void shouldAskEveryCardAndEveryFormatOfASmallNotion() {
        Map<UUID, CardType> small = cards(CardType.FLASHCARD, CardType.FLASHCARD, CardType.TRUE_FALSE);
        List<UUID> smallIds = List.copyOf(small.keySet());
        smallIds.forEach(id -> states.put(id, stableState(30)));

        NotionMastery mastery = NotionMastery.of(small, states, Set.copyOf(smallIds));

        assertThat(mastery.level()).isEqualTo(MasteryLevel.MASTERED);
        assertThat(mastery.nextLevel()).contains(new NextLevel(MasteryLevel.EXPERT, 0, 3, 0, 2, Set.of()));
    }

    @Test
    void shouldOfferNoNextLevelForANotionWithoutCards() {
        NotionMastery mastery = NotionMastery.of(Map.of(), states, Set.of());

        assertThat(mastery.level()).isEqualTo(MasteryLevel.NEW);
        assertThat(mastery.nextLevel()).isEmpty();
    }

    private NotionMastery mastery() {
        return NotionMastery.of(cards, states, recalled);
    }

    private void learning(int... indexes) {
        for (int index : indexes) {
            states.put(ids.get(index), new ReviewState(LearningState.LEARNING, 1, 1, 5, NOW, 1, 0, NOW));
        }
    }

    private void stable(double stabilityDays, int... indexes) {
        for (int index : indexes) {
            states.put(ids.get(index), stableState(stabilityDays));
        }
    }

    private void recall(int... indexes) {
        for (int index : indexes) {
            recalled.add(ids.get(index));
        }
    }

    private static ReviewState stableState(double stabilityDays) {
        return new ReviewState(LearningState.REVIEW, 0, stabilityDays, 5, NOW.plusSeconds(86_400), 4, 0, NOW);
    }

    private static Map<UUID, CardType> cards(CardType... formats) {
        Map<UUID, CardType> cards = new LinkedHashMap<>();
        for (CardType format : formats) {
            cards.put(UUID.randomUUID(), format);
        }
        return cards;
    }
}
