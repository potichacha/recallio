package com.recallio.entitlement.infrastructure;

import com.recallio.entitlement.domain.CodeAttemptRepository;
import com.recallio.entitlement.domain.CodeAttempts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JpaCodeAttemptRepository implements CodeAttemptRepository {

    private static final String USER_ID = "userId";
    private static final String SELECT =
            "select window_started_at, failures from access_code_attempt where user_id = :userId";

    private final EntityManager entityManager;

    public JpaCodeAttemptRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean isBlocked(UUID userId, Instant now) {
        return read(SELECT, userId).stream().anyMatch(attempts -> attempts.isBlockedAt(now));
    }

    @Override
    public void recordFailure(UUID userId, Instant now) {
        CodeAttempts first = CodeAttempts.first(now);
        int inserted = entityManager
                .createNativeQuery("""
                        insert into access_code_attempt (user_id, window_started_at, failures)
                        values (:userId, :startedAt, :failures)
                        on conflict (user_id) do nothing
                        """)
                .setParameter(USER_ID, userId)
                .setParameter("startedAt", first.windowStartedAt())
                .setParameter("failures", first.failures())
                .executeUpdate();
        if (inserted > 0) {
            return;
        }
        CodeAttempts next = read(SELECT + " for update", userId).getFirst().afterFailureAt(now);
        entityManager
                .createNativeQuery("""
                        update access_code_attempt set window_started_at = :startedAt, failures = :failures
                        where user_id = :userId
                        """)
                .setParameter(USER_ID, userId)
                .setParameter("startedAt", next.windowStartedAt())
                .setParameter("failures", next.failures())
                .executeUpdate();
    }

    private List<CodeAttempts> read(String sql, UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery(sql)
                .setParameter(USER_ID, userId)
                .getResultList();
        return rows.stream()
                .map(row -> (Object[]) row)
                .map(row -> new CodeAttempts(SqlValues.instant(row[0]), SqlValues.integer(row[1])))
                .toList();
    }
}
