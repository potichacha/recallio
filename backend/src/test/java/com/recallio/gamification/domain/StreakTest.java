package com.recallio.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class StreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @Test
    void shouldBeZeroWithoutAnyActivity() {
        assertThat(Streak.of(List.of(), TODAY)).isEqualTo(new Streak(0, 0, List.of(), false));
    }

    @Test
    void shouldCountConsecutiveDaysUpToToday() {
        assertThat(Streak.of(run(TODAY, 3), TODAY).days()).isEqualTo(3);
    }

    @Test
    void shouldKeepYesterdaysStreakWhileTodayIsNotOver() {
        assertThat(Streak.of(run(TODAY.minusDays(1), 2), TODAY).days()).isEqualTo(2);
    }

    @Test
    void shouldResetStreakWhenADayIsMissed() {
        assertThat(Streak.of(run(TODAY.minusDays(2), 3), TODAY).days()).isZero();
    }

    @Test
    void shouldOnlyCountTheDaysAfterTheLastGap() {
        var days = List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(3), TODAY.minusDays(4));

        assertThat(Streak.of(days, TODAY).days()).isEqualTo(2);
    }

    @Test
    void shouldCountAcrossMonthsAndYears() {
        LocalDate newYear = LocalDate.of(2027, 1, 1);

        assertThat(Streak.of(run(newYear, 3), newYear).days()).isEqualTo(3);
    }

    @Test
    void shouldEarnAFreezeAfterSevenDaysInARow() {
        Streak streak = Streak.of(run(TODAY, 7), TODAY);

        assertThat(streak.days()).isEqualTo(7);
        assertThat(streak.freezes()).isEqualTo(1);
        assertThat(streak.freezeEarnedToday()).isTrue();
        assertThat(streak.daysToNextFreeze()).hasValue(7);
    }

    @Test
    void shouldCountTheDaysLeftBeforeTheNextFreeze() {
        assertThat(Streak.of(List.of(), TODAY).daysToNextFreeze()).hasValue(7);
        assertThat(Streak.of(run(TODAY, 3), TODAY).daysToNextFreeze()).hasValue(4);
    }

    @Test
    void shouldKeepAtMostTwoFreezesInReserve() {
        Streak streak = Streak.of(run(TODAY, 21), TODAY);

        assertThat(streak.freezes()).isEqualTo(Streak.MAX_FREEZES);
        assertThat(streak.freezeEarnedToday()).isFalse();
        assertThat(streak.daysToNextFreeze()).isEmpty();
    }

    @Test
    void shouldCoverAMissedDayWithAFreeze() {
        // L'objectif atteint les sept jours qui finissent avant-hier, puis hier manqué.
        Streak streak = Streak.of(run(TODAY.minusDays(2), 7), TODAY);

        assertThat(streak.days()).isEqualTo(7);
        assertThat(streak.freezes()).isZero();
        assertThat(streak.frozenDays()).containsExactly(TODAY.minusDays(1));
    }

    @Test
    void shouldNotUseAFreezeForTodayBeforeTheDayIsOver() {
        Streak streak = Streak.of(run(TODAY.minusDays(1), 7), TODAY);

        assertThat(streak.days()).isEqualTo(7);
        assertThat(streak.freezes()).isEqualTo(1);
        assertThat(streak.frozenDays()).isEmpty();
    }

    @Test
    void shouldNotCountAFrozenDayInTheStreak() {
        var days = concat(run(TODAY.minusDays(3), 7), run(TODAY, 2));

        Streak streak = Streak.of(days, TODAY);

        assertThat(streak.days()).isEqualTo(9);
        assertThat(streak.frozenDays()).containsExactly(TODAY.minusDays(2));
    }

    @Test
    void shouldUseOneFreezePerMissedDay() {
        // Quatorze jours, donc deux gels, puis deux jours manqués.
        Streak streak = Streak.of(run(TODAY.minusDays(3), 14), TODAY);

        assertThat(streak.days()).isEqualTo(14);
        assertThat(streak.freezes()).isZero();
        assertThat(streak.frozenDays()).containsExactly(TODAY.minusDays(2), TODAY.minusDays(1));
    }

    @Test
    void shouldResetWhenTheMissedDaysOutnumberTheFreezes() {
        // Quatorze jours, donc deux gels, puis trois jours manqués.
        Streak streak = Streak.of(run(TODAY.minusDays(4), 14), TODAY);

        assertThat(streak.days()).isZero();
        assertThat(streak.freezes()).isZero();
        assertThat(streak.frozenDays()).containsExactly(TODAY.minusDays(3), TODAY.minusDays(2));
    }

    @Test
    void shouldEarnTheNextFreezeAtTheNextMultipleOfSevenDays() {
        // Sept jours, un jour couvert par le gel qu'ils ont rapporté, puis sept jours jusqu'à aujourd'hui.
        var days = concat(run(TODAY.minusDays(8), 7), run(TODAY, 7));

        Streak streak = Streak.of(days, TODAY);

        assertThat(streak.days()).isEqualTo(14);
        assertThat(streak.freezes()).isEqualTo(1);
        assertThat(streak.freezeEarnedToday()).isTrue();
        assertThat(streak.frozenDays()).containsExactly(TODAY.minusDays(7));
    }

    /** Les {@code count} jours consécutifs qui finissent le {@code last}. */
    private static List<LocalDate> run(LocalDate last, int count) {
        return Stream.iterate(last, day -> day.minusDays(1)).limit(count).toList();
    }

    private static List<LocalDate> concat(List<LocalDate> first, List<LocalDate> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
