package com.recallio.review.infrastructure;

import com.recallio.review.domain.exam.MockExam;
import com.recallio.review.domain.exam.MockExam.Submission;
import com.recallio.review.domain.exam.MockExamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class JpaMockExamRepository implements MockExamRepository {

    private static final String COLUMNS =
            "id, user_id, subject_slug, started_at, deadline, submitted_at, correct, total";

    private final EntityManager entityManager;

    public JpaMockExamRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(MockExam exam) {
        entityManager
                .createNativeQuery("""
                        insert into mock_exam (id, user_id, subject_slug, started_at, deadline)
                        values (:id, :userId, :subject, :startedAt, :deadline)
                        """)
                .setParameter("id", exam.id())
                .setParameter("userId", exam.userId())
                .setParameter("subject", exam.subjectSlug())
                .setParameter("startedAt", exam.startedAt())
                .setParameter("deadline", exam.deadline())
                .executeUpdate();
        for (int position = 0; position < exam.cardIds().size(); position++) {
            entityManager
                    .createNativeQuery("""
                            insert into mock_exam_question (exam_id, position, card_id)
                            values (:examId, :position, :cardId)
                            """)
                    .setParameter("examId", exam.id())
                    .setParameter("position", position)
                    .setParameter("cardId", exam.cardIds().get(position))
                    .executeUpdate();
        }
    }

    @Override
    public Optional<MockExam> find(UUID id) {
        List<?> rows = entityManager
                .createNativeQuery("select " + COLUMNS + " from mock_exam where id = :id")
                .setParameter("id", id)
                .getResultList();
        return rows.stream().findFirst().map(row -> toDomain((Object[]) row));
    }

    @Override
    public boolean submit(UUID id, Submission submission) {
        return entityManager
                        .createNativeQuery("""
                                update mock_exam set submitted_at = :at, correct = :correct, total = :total
                                where id = :id and submitted_at is null
                                """)
                        .setParameter("at", submission.submittedAt())
                        .setParameter("correct", submission.correct())
                        .setParameter("total", submission.total())
                        .setParameter("id", id)
                        .executeUpdate()
                > 0;
    }

    @Override
    public List<MockExam> submitted(UUID userId, String subjectSlug, int limit) {
        List<?> rows = entityManager
                .createNativeQuery("select " + COLUMNS + """
                         from mock_exam
                        where user_id = :userId and subject_slug = :subject and submitted_at is not null
                        order by started_at desc
                        """)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .setMaxResults(limit)
                .getResultList();
        return rows.stream().map(row -> toDomain((Object[]) row)).toList();
    }

    private MockExam toDomain(Object[] row) {
        UUID id = (UUID) row[0];
        List<?> questions = entityManager
                .createNativeQuery(
                        "select card_id from mock_exam_question where exam_id = :id order by position", UUID.class)
                .setParameter("id", id)
                .getResultList();
        List<UUID> cardIds = questions.stream().map(UUID.class::cast).toList();
        @Nullable Instant submittedAt = row[5] == null ? null : instant(row[5]);
        @Nullable
        Submission submission = submittedAt == null
                ? null
                : new Submission(submittedAt, ((Number) row[6]).intValue(), ((Number) row[7]).intValue());
        return new MockExam(id, (UUID) row[1], (String) row[2], cardIds, instant(row[3]), instant(row[4]), submission);
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
