package com.recallio.gamification.infrastructure;

import com.recallio.gamification.domain.quest.Quest;
import com.recallio.gamification.domain.quest.QuestRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class JpaQuestRepository implements QuestRepository {

    private final EntityManager entityManager;

    public JpaQuestRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public int advance(UUID userId, Quest quest, LocalDate periodStart, int amount) {
        Number progress = (Number) entityManager
                .createNativeQuery("""
                        insert into quest_progress (user_id, quest, period_start, progress)
                        values (:userId, :quest, :start, :amount)
                        on conflict (user_id, quest, period_start) do update
                            set progress = quest_progress.progress + excluded.progress
                        returning progress
                        """)
                .setParameter("userId", userId)
                .setParameter("quest", quest.name())
                .setParameter("start", periodStart)
                .setParameter("amount", amount)
                .getSingleResult();
        return progress.intValue();
    }

    @Override
    public boolean complete(UUID userId, Quest quest, LocalDate periodStart, Instant completedAt) {
        return entityManager
                        .createNativeQuery("""
                                update quest_progress set completed_at = :completedAt
                                where user_id = :userId and quest = :quest and period_start = :start
                                    and completed_at is null
                                """)
                        .setParameter("completedAt", completedAt)
                        .setParameter("userId", userId)
                        .setParameter("quest", quest.name())
                        .setParameter("start", periodStart)
                        .executeUpdate()
                > 0;
    }

    @Override
    public boolean firstTime(UUID userId, LocalDate periodStart, String item) {
        return entityManager
                        .createNativeQuery("""
                                insert into quest_seen (user_id, period_start, item) values (:userId, :start, :item)
                                on conflict (user_id, period_start, item) do nothing
                                """)
                        .setParameter("userId", userId)
                        .setParameter("start", periodStart)
                        .setParameter("item", item)
                        .executeUpdate()
                > 0;
    }

    @Override
    public Map<QuestKey, QuestState> states(UUID userId, Collection<LocalDate> periodStarts) {
        if (periodStarts.isEmpty()) {
            return Map.of();
        }
        List<?> rows = entityManager
                .createNativeQuery("""
                        select quest, period_start, progress, completed_at from quest_progress
                        where user_id = :userId and period_start in (:starts)
                        """)
                .setParameter("userId", userId)
                .setParameter("starts", List.copyOf(periodStarts))
                .getResultList();
        Map<QuestKey, QuestState> states = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            states.put(
                    new QuestKey(Quest.valueOf((String) columns[0]), localDate(columns[1])),
                    new QuestState(((Number) columns[2]).intValue(), instant(columns[3])));
        }
        return states;
    }

    private static LocalDate localDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }

    /** Selon le pilote, une colonne {@code timestamptz} arrive en {@link Instant}, {@link OffsetDateTime} ou {@link Timestamp}. */
    private static @Nullable Instant instant(@Nullable Object value) {
        return switch (value) {
            case null -> null;
            case Instant instant -> instant;
            case OffsetDateTime dateTime -> dateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("date inattendue : " + value.getClass());
        };
    }
}
