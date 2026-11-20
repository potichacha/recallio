package com.recallio.review.infrastructure;

import com.recallio.review.domain.daily.DailyChallenge;
import com.recallio.review.domain.daily.DailyChallengeAnswer;
import com.recallio.review.domain.daily.DailyChallengeRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaDailyChallengeRepository implements DailyChallengeRepository {

    private static final String CHALLENGE_COLUMNS = "id, subject_slug, day, created_at";
    private static final String ANSWER_COLUMNS = "challenge_id, user_id, position, correct, answered_at, request_id";

    private final EntityManager entityManager;

    public JpaDailyChallengeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<DailyChallenge> find(UUID id) {
        return challenges(entityManager
                        .createNativeQuery("select " + CHALLENGE_COLUMNS + " from daily_challenge where id = :id")
                        .setParameter("id", id)
                        .getResultList())
                .stream()
                .findFirst();
    }

    @Override
    public Optional<DailyChallenge> find(String subjectSlug, LocalDate day) {
        return challenges(entityManager
                        .createNativeQuery("select " + CHALLENGE_COLUMNS
                                + " from daily_challenge where subject_slug = :subject and day = :day")
                        .setParameter("subject", subjectSlug)
                        .setParameter("day", day)
                        .getResultList())
                .stream()
                .findFirst();
    }

    @Override
    public List<DailyChallenge> findByDays(Collection<LocalDate> days) {
        if (days.isEmpty()) {
            return List.of();
        }
        return challenges(entityManager
                .createNativeQuery("select " + CHALLENGE_COLUMNS + " from daily_challenge where day in (:days)")
                .setParameter("days", days)
                .getResultList());
    }

    /**
     * {@code ON CONFLICT DO NOTHING} : si une autre transaction garde au même moment le défi du même sujet et du même
     * jour, PostgreSQL attend qu'elle se termine, puis n'insère rien ; on relit alors le sien.
     */
    @Override
    public DailyChallenge addIfAbsent(DailyChallenge challenge) {
        int inserted = entityManager
                .createNativeQuery("""
                        insert into daily_challenge (id, subject_slug, day, created_at)
                        values (:id, :subject, :day, :createdAt)
                        on conflict (subject_slug, day) do nothing
                        """)
                .setParameter("id", challenge.id())
                .setParameter("subject", challenge.subjectSlug())
                .setParameter("day", challenge.day())
                .setParameter("createdAt", challenge.createdAt())
                .executeUpdate();
        if (inserted == 0) {
            return find(challenge.subjectSlug(), challenge.day()).orElseThrow();
        }
        for (int position = 0; position < challenge.cardIds().size(); position++) {
            entityManager
                    .createNativeQuery("""
                            insert into daily_challenge_question (challenge_id, position, card_id)
                            values (:challengeId, :position, :cardId)
                            """)
                    .setParameter("challengeId", challenge.id())
                    .setParameter("position", position)
                    .setParameter("cardId", challenge.cardIds().get(position))
                    .executeUpdate();
        }
        return challenge;
    }

    @Override
    public Set<UUID> recentCards(String subjectSlug, LocalDate before, int challenges) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select q.card_id from daily_challenge_question q
                        where q.challenge_id in (
                            select id from daily_challenge where subject_slug = :subject and day < :before
                            order by day desc limit :challenges)
                        """, UUID.class)
                .setParameter("subject", subjectSlug)
                .setParameter("before", before)
                .setParameter("challenges", challenges)
                .getResultList();
        return rows.stream().map(UUID.class::cast).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<DailyChallengeAnswer> answers(UUID userId, Collection<UUID> challengeIds) {
        if (challengeIds.isEmpty()) {
            return List.of();
        }
        List<?> rows = entityManager
                .createNativeQuery("select " + ANSWER_COLUMNS + """
                         from daily_challenge_answer
                        where user_id = :userId and challenge_id in (:challenges)
                        order by challenge_id, position
                        """)
                .setParameter("userId", userId)
                .setParameter("challenges", challengeIds)
                .getResultList();
        return rows.stream().map(row -> toAnswer((Object[]) row)).toList();
    }

    @Override
    public Optional<DailyChallengeAnswer> findAnswer(UUID requestId) {
        List<?> rows = entityManager
                .createNativeQuery(
                        "select " + ANSWER_COLUMNS + " from daily_challenge_answer where request_id = :requestId")
                .setParameter("requestId", requestId)
                .getResultList();
        return rows.stream().findFirst().map(row -> toAnswer((Object[]) row));
    }

    /**
     * {@code ON CONFLICT DO NOTHING}, sans cible : la question déjà répondue (la clé primaire) comme l'identifiant de
     * requête déjà servi (son unicité) laissent la réponse d'avant en place.
     */
    @Override
    public boolean addAnswerIfAbsent(DailyChallengeAnswer answer) {
        return entityManager
                        .createNativeQuery("""
                                insert into daily_challenge_answer
                                    (challenge_id, user_id, position, correct, answered_at, request_id)
                                values (:challengeId, :userId, :position, :correct, :answeredAt, :requestId)
                                on conflict do nothing
                                """)
                        .setParameter("challengeId", answer.challengeId())
                        .setParameter("userId", answer.userId())
                        .setParameter("position", answer.position())
                        .setParameter("correct", answer.correct())
                        .setParameter("answeredAt", answer.answeredAt())
                        .setParameter("requestId", answer.requestId())
                        .executeUpdate()
                == 1;
    }

    @Override
    public Map<Integer, Integer> playersByScore(UUID challengeId, Collection<Integer> positions) {
        if (positions.isEmpty()) {
            return Map.of();
        }
        List<?> rows = entityManager
                .createNativeQuery("""
                        select score, count(*) from (
                            select count(*) filter (where correct) as score
                            from daily_challenge_answer
                            where challenge_id = :challengeId and position in (:positions)
                            group by user_id
                            having count(*) = :questions
                        ) finished
                        group by score
                        """)
                .setParameter("challengeId", challengeId)
                .setParameter("positions", positions)
                .setParameter("questions", positions.size())
                .getResultList();
        Map<Integer, Integer> players = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            players.put(((Number) columns[0]).intValue(), ((Number) columns[1]).intValue());
        }
        return players;
    }

    /** Chaque défi, avec ses questions lues en une seule requête. */
    private List<DailyChallenge> challenges(List<?> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Object[]> challenges = rows.stream().map(Object[].class::cast).toList();
        List<UUID> ids = challenges.stream().map(row -> (UUID) row[0]).toList();
        List<?> questions =
                entityManager.createNativeQuery("""
                        select challenge_id, card_id from daily_challenge_question
                        where challenge_id in (:ids)
                        order by challenge_id, position
                        """).setParameter("ids", ids).getResultList();
        Map<UUID, List<UUID>> cardsByChallenge = questions.stream()
                .map(Object[].class::cast)
                .collect(Collectors.groupingBy(
                        row -> (UUID) row[0], Collectors.mapping(row -> (UUID) row[1], Collectors.toList())));
        return challenges.stream()
                .map(row -> new DailyChallenge(
                        (UUID) row[0],
                        (String) row[1],
                        toLocalDate(row[2]),
                        cardsByChallenge.getOrDefault((UUID) row[0], List.of()),
                        instant(row[3])))
                .toList();
    }

    private static DailyChallengeAnswer toAnswer(Object[] row) {
        return new DailyChallengeAnswer(
                (UUID) row[0], (UUID) row[1], ((Number) row[2]).intValue(), (Boolean) row[3], instant(row[4]), (UUID)
                        row[5]);
    }

    /** Selon le pilote, une colonne {@code date} arrive en {@link LocalDate} ou en {@link java.sql.Date}. */
    private static LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }

    private static Instant instant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case OffsetDateTime dateTime -> dateTime.toInstant();
            case Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("date inattendue : " + value.getClass());
        };
    }
}
