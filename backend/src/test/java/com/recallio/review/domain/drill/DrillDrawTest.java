package com.recallio.review.domain.drill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.recallio.review.domain.drill.DrillDraw.Candidate;
import com.recallio.review.domain.drill.DrillDraw.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DrillDrawTest {

    private static final int NOTIONS = 30;
    private static final int CARDS_PER_NOTION = 4;
    private static final int PAGE = 10;
    private static final long SEED = 42;

    /** Trente notions de quatre cartes : 120 cartes. */
    private static final List<Candidate> CANDIDATES = IntStream.range(0, NOTIONS * CARDS_PER_NOTION)
            .mapToObj(number -> card(number, number % NOTIONS))
            .toList();

    private static final Map<UUID, UUID> NOTION_OF =
            CANDIDATES.stream().collect(Collectors.toMap(Candidate::cardId, Candidate::notionId));

    private static Candidate card(int number, int notion) {
        return new Candidate(new UUID(0, number), new UUID(1, notion));
    }

    @Test
    void shouldDrawTheSamePageFromTheSameSeed() {
        Page first = DrillDraw.page(CANDIDATES, SEED, 0, PAGE);

        assertThat(first.cardIds()).hasSize(PAGE);
        assertThat(first.total()).isEqualTo(CANDIDATES.size());
        assertThat(first.nextOffset()).isEqualTo(PAGE);
        assertThat(DrillDraw.page(CANDIDATES, SEED, 0, PAGE)).isEqualTo(first);
    }

    @Test
    void shouldNotDependOnTheOrderOfTheCandidates() {
        List<Candidate> reversed = new ArrayList<>(CANDIDATES);
        Collections.reverse(reversed);

        assertThat(DrillDraw.page(reversed, SEED, 30, PAGE)).isEqualTo(DrillDraw.page(CANDIDATES, SEED, 30, PAGE));
    }

    @Test
    void shouldDrawAnotherOrderFromAnotherSeed() {
        assertThat(DrillDraw.page(CANDIDATES, SEED + 1, 0, PAGE).cardIds())
                .isNotEqualTo(DrillDraw.page(CANDIDATES, SEED, 0, PAGE).cardIds());
    }

    @Test
    void shouldAskEveryCardOnceBeforeAskingOneAgain() {
        List<UUID> lap = cardsUntil(CANDIDATES, CANDIDATES.size());

        assertThat(lap)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(
                        CANDIDATES.stream().map(Candidate::cardId).toList());
    }

    @Test
    void shouldAskOnlyOneCardOfANotionOnAPage() {
        long offset = 0;
        while (offset < CANDIDATES.size()) {
            Page page = DrillDraw.page(CANDIDATES, SEED, offset, PAGE);

            assertThat(page.cardIds()).isNotEmpty();
            assertThat(page.cardIds()).extracting(NOTION_OF::get).doesNotHaveDuplicates();
            offset = page.nextOffset();
        }
    }

    /** Deux notions seulement : la page s'arrête avant la deuxième carte d'une notion, et la suivante la reprend. */
    @Test
    void shouldCutThePageShortRatherThanRepeatANotionOrSkipACard() {
        List<Candidate> few = List.of(card(1, 1), card(2, 1), card(3, 1), card(4, 2), card(5, 2));

        Page first = DrillDraw.page(few, SEED, 0, PAGE);
        assertThat(first.cardIds()).hasSize(2);
        assertThat(first.nextOffset()).isEqualTo(2);
        assertThat(first.total()).isEqualTo(5);

        assertThat(cardsUntil(few, few.size()))
                .containsExactlyInAnyOrderElementsOf(
                        few.stream().map(Candidate::cardId).toList());
    }

    @Test
    void shouldStopAPageAtTheEndOfALapAndStartAnotherOrder() {
        int total = CANDIDATES.size();

        Page last = DrillDraw.page(CANDIDATES, SEED, total - 3, PAGE);
        assertThat(last.cardIds()).hasSize(3);
        assertThat(last.nextOffset()).isEqualTo(total);

        List<UUID> firstLap = cardsUntil(CANDIDATES, total);
        List<UUID> twoLaps = cardsUntil(CANDIDATES, 2L * total);
        List<UUID> secondLap = twoLaps.subList(total, twoLaps.size());
        assertThat(secondLap).containsExactlyInAnyOrderElementsOf(firstLap).isNotEqualTo(firstLap);
    }

    @Test
    void shouldGiveAnEmptyPageWhenNoCardIsEligible() {
        assertThat(DrillDraw.page(List.of(), SEED, 40, PAGE)).isEqualTo(new Page(List.of(), 0, 0));
    }

    @Test
    void shouldRefuseANegativeOffsetOrAnEmptyPage() {
        assertThatIllegalArgumentException().isThrownBy(() -> DrillDraw.page(CANDIDATES, SEED, -1, PAGE));
        assertThatIllegalArgumentException().isThrownBy(() -> DrillDraw.page(CANDIDATES, SEED, 0, 0));
    }

    /** Les cartes servies page après page, en suivant {@code nextOffset}, jusqu'à cet offset. */
    private static List<UUID> cardsUntil(List<Candidate> candidates, long end) {
        List<UUID> served = new ArrayList<>();
        long offset = 0;
        while (offset < end) {
            Page page = DrillDraw.page(candidates, SEED, offset, PAGE);
            served.addAll(page.cardIds());
            offset = page.nextOffset();
        }
        assertThat(offset).isEqualTo(end);
        return served;
    }
}
