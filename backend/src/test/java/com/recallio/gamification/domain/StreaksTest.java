package com.recallio.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreaksTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

    @Test
    void shouldBeZeroWithoutAnyActivity() {
        assertThat(Streaks.current(List.of(), TODAY)).isZero();
    }

    @Test
    void shouldCountConsecutiveDaysUpToToday() {
        var days = List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));

        assertThat(Streaks.current(days, TODAY)).isEqualTo(3);
    }

    @Test
    void shouldKeepYesterdaysStreakWhileTodayIsNotOver() {
        var days = List.of(TODAY.minusDays(1), TODAY.minusDays(2));

        assertThat(Streaks.current(days, TODAY)).isEqualTo(2);
    }

    @Test
    void shouldResetStreakWhenADayIsMissed() {
        var days = List.of(TODAY.minusDays(2), TODAY.minusDays(3), TODAY.minusDays(4));

        assertThat(Streaks.current(days, TODAY)).isZero();
    }

    @Test
    void shouldOnlyCountTheDaysAfterTheLastGap() {
        var days = List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(3), TODAY.minusDays(4));

        assertThat(Streaks.current(days, TODAY)).isEqualTo(2);
    }

    @Test
    void shouldCountAcrossMonthsAndYears() {
        LocalDate newYear = LocalDate.of(2027, 1, 1);
        var days = List.of(newYear, newYear.minusDays(1), newYear.minusDays(2));

        assertThat(Streaks.current(days, newYear)).isEqualTo(3);
    }
}
