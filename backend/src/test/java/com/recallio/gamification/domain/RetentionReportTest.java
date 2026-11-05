package com.recallio.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.gamification.domain.RetentionReport.DayRetention;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetentionReportTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-11-30");

    @Test
    void shouldCountThePlayersBackOnTheExactDayAmongThoseWhoStartedLongEnoughAgo() {
        LocalDate start = LocalDate.parse("2026-10-31");
        var report = RetentionReport.of(
                Map.of(
                        // Revenue à J1 et à J7, pas à J30.
                        UUID.randomUUID(), Set.of(start, start.plusDays(1), start.plusDays(7)),
                        // Revenue à J30 seulement.
                        UUID.randomUUID(), Set.of(start, start.plusDays(30)),
                        // Commencé il y a 3 jours : compté à J1 seulement, où il n'est pas revenu.
                        UUID.randomUUID(), Set.of(TODAY.minusDays(3))),
                TODAY);

        assertThat(report.players()).isEqualTo(3);
        assertThat(report.days())
                .containsExactly(new DayRetention(1, 3, 1), new DayRetention(7, 2, 1), new DayRetention(30, 2, 1));
        assertThat(report.days().get(1).percent()).isEqualTo(50);
    }

    @Test
    void shouldReportNothingBeforeAnyoneIsEligible() {
        var report = RetentionReport.of(Map.of(UUID.randomUUID(), Set.of(TODAY)), TODAY);

        assertThat(report.days()).allSatisfy(day -> assertThat(day.percent()).isZero());
        assertThat(report.days()).extracting(DayRetention::eligible).containsOnly(0);
    }
}
