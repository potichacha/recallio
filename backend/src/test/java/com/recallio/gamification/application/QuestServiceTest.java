package com.recallio.gamification.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.gamification.domain.quest.Quest;
import com.recallio.gamification.domain.quest.QuestBoard;
import com.recallio.gamification.domain.quest.QuestPeriod;
import com.recallio.gamification.domain.quest.QuestRepository;
import com.recallio.user.UserRef;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestServiceTest {

    // Le lundi 16 novembre 2026, à 10 h UTC : le jour et la semaine commencent à la même date.
    private static final Clock MONDAY = Clock.fixed(Instant.parse("2026-11-16T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate MONDAY_DATE = LocalDate.of(2026, 11, 16);

    private final InMemoryQuests quests = new InMemoryQuests();
    private final QuestService service = new QuestService(quests, MONDAY);
    private final UserRef user = new UserRef(UUID.randomUUID(), ZoneId.of("UTC"));

    @Test
    void shouldGiveTheQuestsOnAMondayWhenTheWeekStartsOnTheSameDay() {
        QuestService.Board board = service.board(user);

        assertThat(quests.askedPeriodStarts).containsExactly(MONDAY_DATE);
        assertThat(board.daily()).isNotEmpty();
        assertThat(board.weekly()).isNotEmpty();
        assertThat(board.dailyEndsOn()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(board.weeklyEndsOn()).isEqualTo(LocalDate.of(2026, 11, 23));
    }

    @Test
    void shouldShowTheProgressOfTheDayAndOfTheWeekThatBothStartOnAMonday() {
        Quest daily = QuestBoard.of(user.id(), QuestPeriod.DAY, MONDAY_DATE).getFirst();
        Quest weekly = QuestBoard.of(user.id(), QuestPeriod.WEEK, MONDAY_DATE).getFirst();
        quests.states.put(new QuestRepository.QuestKey(daily, MONDAY_DATE), new QuestRepository.QuestState(1, null));
        quests.states.put(new QuestRepository.QuestKey(weekly, MONDAY_DATE), new QuestRepository.QuestState(1, null));

        QuestService.Board board = service.board(user);

        assertThat(board.daily().getFirst().progress()).isEqualTo(1);
        assertThat(board.weekly().getFirst().progress()).isEqualTo(1);
    }

    /** Ne sert que la lecture des progressions, la seule que fait le tableau des quêtes. */
    private static final class InMemoryQuests implements QuestRepository {

        private final Map<QuestKey, QuestState> states = new HashMap<>();
        private List<LocalDate> askedPeriodStarts = List.of();

        @Override
        public int advance(UUID userId, Quest quest, LocalDate periodStart, int amount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean complete(UUID userId, Quest quest, LocalDate periodStart, Instant completedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean firstTime(UUID userId, LocalDate periodStart, String item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<QuestKey, QuestState> states(UUID userId, Collection<LocalDate> periodStarts) {
            askedPeriodStarts = List.copyOf(periodStarts);
            return Map.copyOf(states);
        }
    }
}
