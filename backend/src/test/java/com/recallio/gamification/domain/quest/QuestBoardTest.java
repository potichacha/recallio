package com.recallio.gamification.domain.quest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestBoardTest {

    private static final UUID USER = UUID.fromString("0190a000-0000-7000-8000-000000000001");
    private static final LocalDate THURSDAY = LocalDate.of(2026, 11, 5);

    @Test
    void shouldGiveThreeQuestsOfTheDayAndTwoOfTheWeek() {
        List<Quest> daily = QuestBoard.of(USER, QuestPeriod.DAY, THURSDAY);
        List<Quest> weekly = QuestBoard.of(USER, QuestPeriod.WEEK, THURSDAY);

        assertThat(daily).hasSize(3).doesNotHaveDuplicates().allMatch(quest -> quest.period() == QuestPeriod.DAY);
        assertThat(weekly).hasSize(2).doesNotHaveDuplicates().allMatch(quest -> quest.period() == QuestPeriod.WEEK);
    }

    @Test
    void shouldKeepTheSameQuestsWithinAPeriodAndChangeThemFromOneToTheNext() {
        assertThat(QuestBoard.of(USER, QuestPeriod.DAY, THURSDAY))
                .isEqualTo(QuestBoard.of(USER, QuestPeriod.DAY, THURSDAY))
                .isNotEqualTo(QuestBoard.of(USER, QuestPeriod.DAY, THURSDAY.plusDays(1)));
        assertThat(QuestBoard.of(USER, QuestPeriod.WEEK, THURSDAY))
                .isEqualTo(QuestBoard.of(USER, QuestPeriod.WEEK, THURSDAY.minusDays(3)))
                .isNotEqualTo(QuestBoard.of(USER, QuestPeriod.WEEK, THURSDAY.plusWeeks(1)));
    }

    @Test
    void shouldStartAWeekOnMondayInTheUsersCalendar() {
        assertThat(QuestPeriod.WEEK.start(THURSDAY)).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(QuestPeriod.WEEK.end(THURSDAY)).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(QuestPeriod.DAY.end(THURSDAY)).isEqualTo(THURSDAY.plusDays(1));
    }
}
