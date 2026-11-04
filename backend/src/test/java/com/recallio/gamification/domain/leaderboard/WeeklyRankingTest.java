package com.recallio.gamification.domain.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.gamification.domain.leaderboard.WeeklyRanking.MemberWeek;
import com.recallio.gamification.domain.leaderboard.WeeklyRanking.Standing;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WeeklyRankingTest {

    @Test
    void shouldRankRegularityAboveXp() {
        var steady = week(5, 100, 50);
        var burst = week(1, 900, 450);

        assertThat(WeeklyRanking.rank(List.of(burst, steady)))
                .containsExactly(new Standing(1, steady), new Standing(2, burst));
    }

    @Test
    void shouldBreakTiesWithXpThenCards() {
        var moreXp = week(3, 60, 20);
        var moreCards = week(3, 50, 40);
        var fewerCards = week(3, 50, 30);

        assertThat(WeeklyRanking.rank(List.of(fewerCards, moreCards, moreXp)))
                .extracting(standing -> standing.member())
                .containsExactly(moreXp, moreCards, fewerCards);
    }

    @Test
    void shouldShareTheRankOfExAequo() {
        var first = week(4, 80, 40);
        var tiedA = week(2, 30, 10);
        var tiedB = week(2, 30, 10);
        var last = week(0, 0, 0);

        assertThat(WeeklyRanking.rank(List.of(last, tiedA, first, tiedB)))
                .extracting(Standing::rank)
                .containsExactly(1, 2, 2, 4);
    }

    @Test
    void shouldRunFromMondayToSunday() {
        Week week = Week.containing(LocalDate.parse("2026-11-01")); // un dimanche
        ZoneId paris = ZoneId.of("Europe/Paris");

        assertThat(week.monday()).isEqualTo(LocalDate.parse("2026-10-26"));
        assertThat(week.sunday()).isEqualTo(LocalDate.parse("2026-11-01"));
        assertThat(Week.containing(LocalDate.parse("2026-10-26"))).isEqualTo(week);
        assertThat(week.startIn(paris)).isEqualTo(Instant.parse("2026-10-25T23:00:00Z"));
        assertThat(week.endIn(paris)).isEqualTo(Instant.parse("2026-11-01T23:00:00Z"));
        assertThatThrownBy(() -> new Week(LocalDate.parse("2026-10-27"))).isInstanceOf(IllegalArgumentException.class);
    }

    private static MemberWeek week(int regularDays, long xp, int cards) {
        return new MemberWeek(UUID.randomUUID(), regularDays, xp, cards);
    }
}
