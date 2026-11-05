package com.recallio.gamification.application;

import com.recallio.gamification.domain.quest.Quest;
import com.recallio.gamification.domain.quest.QuestBoard;
import com.recallio.gamification.domain.quest.QuestPeriod;
import com.recallio.gamification.domain.quest.QuestRepository;
import com.recallio.gamification.domain.quest.QuestRepository.QuestKey;
import com.recallio.gamification.domain.quest.QuestRepository.QuestState;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cas d'usage : les quêtes du jour et de la semaine d'un utilisateur, et où il en est (ADR 0040). */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class QuestService {

    private final QuestRepository quests;
    private final Clock clock;

    public QuestService(QuestRepository quests, Clock clock) {
        this.quests = quests;
        this.clock = clock;
    }

    public Board board(UserRef user) {
        LocalDate today = LocalDate.now(clock.withZone(user.timezone()));
        Map<QuestKey, QuestState> states = quests.states(user.id(), Set.of(today, QuestPeriod.WEEK.start(today)));
        return new Board(
                views(user, QuestPeriod.DAY, today, states),
                QuestPeriod.DAY.end(today),
                views(user, QuestPeriod.WEEK, today, states),
                QuestPeriod.WEEK.end(today));
    }

    private static List<QuestView> views(
            UserRef user, QuestPeriod period, LocalDate today, Map<QuestKey, QuestState> states) {
        LocalDate start = period.start(today);
        return QuestBoard.of(user.id(), period, today).stream()
                .map(quest -> {
                    QuestState state = states.get(new QuestKey(quest, start));
                    int progress = state == null ? 0 : Math.min(state.progress(), quest.target());
                    return new QuestView(quest, progress, state != null && state.completedAt() != null);
                })
                .toList();
    }

    /**
     * @param dailyEndsOn le jour où les quêtes du jour changent, dans le fuseau de l'utilisateur
     * @param weeklyEndsOn le lundi où les quêtes de la semaine changent
     */
    public record Board(List<QuestView> daily, LocalDate dailyEndsOn, List<QuestView> weekly, LocalDate weeklyEndsOn) {

        public Board {
            daily = List.copyOf(daily);
            weekly = List.copyOf(weekly);
        }
    }

    /** @param progress plafonnée à l'objectif de la quête */
    public record QuestView(Quest quest, int progress, boolean completed) {}
}
