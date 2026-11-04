package com.recallio.review.domain.track;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import com.recallio.review.domain.track.NotionMastery.StableCards;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MasteryCountsTest {

    private static final Set<CardType> COURSE = Set.of(CardType.FLASHCARD);
    private static final StableCards NONE = new StableCards(0, Set.of());
    private static final StableCards ONE = new StableCards(1, COURSE);

    /** Des notions d'une carte, une à chaque niveau, et deux expertes. */
    private static final List<NotionMastery> NOTIONS = List.of(
            new NotionMastery(1, COURSE, 0, 0, Set.of(), NONE, NONE),
            new NotionMastery(1, COURSE, 1, 0, Set.of(), NONE, NONE),
            new NotionMastery(1, COURSE, 1, 1, COURSE, NONE, NONE),
            new NotionMastery(1, COURSE, 1, 1, COURSE, ONE, NONE),
            new NotionMastery(1, COURSE, 1, 1, COURSE, ONE, ONE),
            new NotionMastery(1, COURSE, 1, 1, COURSE, ONE, ONE));

    @Test
    void shouldCountEachNotionAtItsLevelAndAtEveryLevelBelow() {
        assertThat(NOTIONS)
                .extracting(NotionMastery::level)
                .containsExactly(
                        MasteryLevel.NEW,
                        MasteryLevel.DISCOVERED,
                        MasteryLevel.ACQUIRED,
                        MasteryLevel.MASTERED,
                        MasteryLevel.EXPERT,
                        MasteryLevel.EXPERT);

        assertThat(MasteryCounts.of(NOTIONS)).isEqualTo(new MasteryCounts(6, 5, 4, 3, 2));
    }

    @Test
    void shouldCountNothingWithoutNotions() {
        assertThat(MasteryCounts.of(List.of())).isEqualTo(MasteryCounts.NONE);
    }
}
