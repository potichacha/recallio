package com.recallio.review.infrastructure;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardType;
import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import com.recallio.review.domain.Rating;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * L'usage vu par le module review (ADR 0076) : le journal de révision, le Tour, les examens blancs, les défis du jour,
 * le test de niveau et le compte à rebours. Le journal ne connaît que l'identifiant des cartes : leur sujet et leur
 * format viennent de la façade du module content, jamais de ses tables.
 */
@ApplicationScoped
public class ReviewUsageStatistics implements UsageStatistics {

    /** La note à partir de laquelle une réponse est juste ({@link Rating#isRecalled()}). */
    private static final int RECALLED = Rating.GOOD.value();

    /** Le nombre de cartes dont on demande le sujet à la fois : sous la limite des paramètres d'une requête. */
    private static final int SUBJECT_LOOKUP_BATCH = 10_000;

    private final UsageStatisticsSql sql;
    private final CardDirectory cards;

    public ReviewUsageStatistics(UsageStatisticsSql sql, CardDirectory cards) {
        this.sql = sql;
        this.cards = cards;
    }

    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        Map<UsageFeature, Set<UUID>> users = new EnumMap<>(UsageFeature.class);
        users.put(UsageFeature.REVIEW, sql.usersBetween("""
                select distinct user_id from review_log
                where reviewed_at >= :from and reviewed_at < :to
                """, from, to));
        users.put(UsageFeature.CODING_EXERCISE, exerciseSubmitters(from, to));
        users.put(UsageFeature.TOUR, sql.usersBetween("""
                select distinct user_id from tour_progress
                where validated_at >= :from and validated_at < :to
                """, from, to));
        users.put(UsageFeature.MOCK_EXAM, sql.usersBetween("""
                select distinct user_id from mock_exam
                where started_at >= :from and started_at < :to
                """, from, to));
        users.put(UsageFeature.DAILY_CHALLENGE, sql.usersBetween("""
                select distinct user_id from daily_challenge_answer
                where answered_at >= :from and answered_at < :to
                """, from, to));
        users.put(UsageFeature.LEVEL_CHECK, sql.usersBetween("""
                select distinct user_id from known_notion
                where known_at >= :from and known_at < :to
                """, from, to));
        // Une date d'entretien se change et se retire : la ligne dit l'état du jour, pas son histoire.
        users.put(UsageFeature.INTERVIEW_COUNTDOWN, sql.users("select distinct user_id from interview_plan"));
        return users;
    }

    /**
     * Un exercice rendu en révision est une ligne du journal comme une autre : on le reconnaît à sa carte. Seuls les
     * exercices encore actifs des sujets publiés se reconnaissent ainsi.
     */
    private Set<UUID> exerciseSubmitters(Instant from, Instant to) {
        UUID[] exercises =
                cards.activeCards(new CardFilter(null, null, Set.of(), Set.of(CardType.CODING_EXERCISE))).stream()
                        .map(ActiveCard::cardId)
                        .toArray(UUID[]::new);
        if (exercises.length == 0) {
            return Set.of();
        }
        return sql.users(sql.between("""
                        select distinct user_id from review_log
                        where reviewed_at >= :from and reviewed_at < :to
                          and card_id = any (cast(:cardIds as uuid[]))
                        """, from, to).setParameter("cardIds", exercises));
    }

    @Override
    public Map<DailyCount, Map<LocalDate, Long>> dailyCounts(Instant from, Instant to) {
        return Map.of(DailyCount.TOUR_VALIDATIONS, sql.countsByDay("""
                select cast(validated_at at time zone 'UTC' as date) as day, count(*)
                from tour_progress
                where validated_at >= :from and validated_at < :to
                group by day
                """, from, to));
    }

    /** Le sujet de chaque carte révisée sur la période est rendu à la base, qui compte alors par sujet. */
    @Override
    public Map<SubjectCount, Map<String, Long>> subjectCounts(Instant from, Instant to) {
        Map<UUID, String> subjects = subjectsOfReviewedCards(from, to);
        Map<SubjectCount, Map<String, Long>> counts = new EnumMap<>(SubjectCount.class);
        counts.put(SubjectCount.REVIEWERS, new HashMap<>());
        counts.put(SubjectCount.REVIEWS, new HashMap<>());
        counts.put(SubjectCount.RECALLED, new HashMap<>());
        if (subjects.isEmpty()) {
            return counts;
        }
        List<UUID> cardIds = List.copyOf(subjects.keySet());
        List<?> rows = sql.between("""
                        select card.subject_slug,
                               count(distinct log.user_id),
                               count(*),
                               count(*) filter (where log.rating >= :recalled)
                        from review_log log
                        join unnest(cast(:cardIds as uuid[]), cast(:subjects as text[])) as card (card_id, subject_slug)
                          on card.card_id = log.card_id
                        where log.reviewed_at >= :from and log.reviewed_at < :to
                        group by card.subject_slug
                        """, from, to)
                .setParameter("recalled", RECALLED)
                .setParameter("cardIds", cardIds.toArray(UUID[]::new))
                .setParameter("subjects", cardIds.stream().map(subjects::get).toArray(String[]::new))
                .getResultList();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            String subject = (String) columns[0];
            counts.get(SubjectCount.REVIEWERS).put(subject, ((Number) columns[1]).longValue());
            counts.get(SubjectCount.REVIEWS).put(subject, ((Number) columns[2]).longValue());
            counts.get(SubjectCount.RECALLED).put(subject, ((Number) columns[3]).longValue());
        }
        return counts;
    }

    private Map<UUID, String> subjectsOfReviewedCards(Instant from, Instant to) {
        List<UUID> reviewed = new ArrayList<>();
        for (Object cardId : sql.between("""
                        select distinct card_id from review_log
                        where reviewed_at >= :from and reviewed_at < :to
                        """, from, to).getResultList()) {
            reviewed.add((UUID) cardId);
        }
        Map<UUID, String> subjects = new HashMap<>();
        for (int start = 0; start < reviewed.size(); start += SUBJECT_LOOKUP_BATCH) {
            int end = Math.min(start + SUBJECT_LOOKUP_BATCH, reviewed.size());
            subjects.putAll(cards.subjectsOf(reviewed.subList(start, end)));
        }
        return subjects;
    }

    @Override
    public List<CardAnswers> mostFailedCards(Instant from, Instant to, int minAnswers, int limit) {
        List<?> rows = sql.between("""
                        select card_id,
                               count(*) as answers,
                               count(*) filter (where rating < :recalled) as failures
                        from review_log
                        where reviewed_at >= :from and reviewed_at < :to
                        group by card_id
                        having count(*) >= :minAnswers and count(*) filter (where rating < :recalled) > 0
                        order by cast(count(*) filter (where rating < :recalled) as double precision) / count(*) desc,
                                 answers desc,
                                 card_id
                        limit :limit
                        """, from, to)
                .setParameter("recalled", RECALLED)
                .setParameter("minAnswers", minAnswers)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(Object[].class::cast)
                .map(row ->
                        new CardAnswers((UUID) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .toList();
    }

    @Override
    public Map<UUID, Long> tourStops() {
        return sql.countsByKey("select notion_id, count(*) from tour_last_page group by notion_id", UUID.class);
    }
}
