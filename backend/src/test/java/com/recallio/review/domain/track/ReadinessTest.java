package com.recallio.review.domain.track;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardType;
import com.recallio.review.domain.track.NotionMastery.StableCards;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReadinessTest {

    private static final Set<CardType> COURSE = Set.of(CardType.FLASHCARD);
    private static final StableCards NONE = new StableCards(0, Set.of());
    private static final StableCards ONE = new StableCards(1, COURSE);

    private static final NotionMastery NEW = new NotionMastery(1, COURSE, 0, 0, Set.of(), NONE, NONE);
    private static final NotionMastery ACQUIRED = new NotionMastery(1, COURSE, 1, 1, COURSE, NONE, NONE);
    private static final NotionMastery MASTERED = new NotionMastery(1, COURSE, 1, 1, COURSE, ONE, NONE);
    private static final NotionMastery EXPERT = new NotionMastery(1, COURSE, 1, 1, COURSE, ONE, ONE);

    @Test
    void shouldCountAnAcquiredNotionForHalfAndAMasteredOneInFull() {
        Readiness readiness = Readiness.of(List.of(NEW, ACQUIRED, MASTERED, EXPERT));

        assertThat(readiness).isEqualTo(new Readiness(4, 3, 2));
        // (3 + 2) / (2 × 4) = 62,5 %, arrondi à l'inférieur.
        assertThat(readiness.percent()).isEqualTo(62);
        assertThat(readiness.ready()).isFalse();
    }

    @Test
    void shouldBeReadyFromEightyPercent() {
        List<NotionMastery> fourMasteredOneAcquired = List.of(MASTERED, MASTERED, MASTERED, EXPERT, ACQUIRED);

        assertThat(Readiness.of(fourMasteredOneAcquired).percent()).isEqualTo(90);
        assertThat(Readiness.of(fourMasteredOneAcquired).ready()).isTrue();
        assertThat(Readiness.of(Collections.nCopies(5, ACQUIRED)).percent()).isEqualTo(50);
        assertThat(Readiness.of(List.of(MASTERED, MASTERED, MASTERED, ACQUIRED, ACQUIRED))
                        .percent())
                .isEqualTo(Readiness.READY_PERCENT);
    }

    @Test
    void shouldNeverBeReadyWithoutEssentialNotions() {
        assertThat(Readiness.of(List.of())).isEqualTo(Readiness.NONE);
        assertThat(Readiness.NONE.percent()).isZero();
        assertThat(Readiness.NONE.ready()).isFalse();
    }
}
