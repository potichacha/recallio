package com.recallio.entitlement.infrastructure;

import com.recallio.entitlement.domain.AccessCodeBatch;
import com.recallio.entitlement.domain.AccessCodeRepository;
import com.recallio.entitlement.domain.BatchSummary;
import com.recallio.entitlement.domain.RedeemableCode;
import com.recallio.platform.domain.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Un module simple, aux requêtes natives, sans entité (CLAUDE.md, section 4.3). */
@ApplicationScoped
public class JpaAccessCodeRepository implements AccessCodeRepository {

    private static final String BATCH_ID = "batchId";

    /** Les colonnes d'un lot, dans l'ordre que lit {@link #batch}. */
    private static final String BATCH_COLUMNS =
            "b.id, b.label, b.plus_days, b.max_redemptions_per_code, b.expires_at, b.created_at, b.revoked_at";

    private static final int BATCH_COLUMN_COUNT = 7;

    private static final String SUMMARIES = "select " + BATCH_COLUMNS + """
            , count(c.id), count(c.id) filter (where c.redemptions > 0), coalesce(sum(c.redemptions), 0)
            from access_code_batch b left join access_code c on c.batch_id = b.id
            """;

    private static final String CODE_BY_FINGERPRINT = "select c.id, c.redemptions, " + BATCH_COLUMNS + """
             from access_code c join access_code_batch b on b.id = c.batch_id
            where c.code_hash = :fingerprint
            """;

    private final EntityManager entityManager;

    public JpaAccessCodeRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void add(AccessCodeBatch batch, List<NewCode> codes) {
        // Une date absente ne se lie pas dans une requête native : la colonne garde alors sa valeur par défaut, NULL.
        Instant expiresAt = batch.expiresAt();
        Query insert = entityManager
                .createNativeQuery(
                        """
                        insert into access_code_batch (id, label, plus_days, max_redemptions_per_code, created_at%s)
                        values (:id, :label, :plusDays, :maxRedemptions, :createdAt%s)
                        """.formatted(expiresAt == null ? "" : ", expires_at", expiresAt == null ? "" : ", :expiresAt"))
                .setParameter("id", batch.id())
                .setParameter("label", batch.label())
                .setParameter("plusDays", batch.plusDays())
                .setParameter("maxRedemptions", batch.maxRedemptionsPerCode())
                .setParameter("createdAt", batch.createdAt());
        if (expiresAt != null) {
            insert.setParameter("expiresAt", expiresAt);
        }
        insert.executeUpdate();
        for (NewCode code : codes) {
            entityManager
                    .createNativeQuery("""
                            insert into access_code (id, batch_id, code_hash, suffix)
                            values (:id, :batchId, :fingerprint, :suffix)
                            """)
                    .setParameter("id", code.id())
                    .setParameter(BATCH_ID, batch.id())
                    .setParameter("fingerprint", code.fingerprint())
                    .setParameter("suffix", code.suffix())
                    .executeUpdate();
        }
    }

    @Override
    public Page<BatchSummary> list(int page, int size) {
        List<?> rows = entityManager
                .createNativeQuery(SUMMARIES + " group by b.id order by b.created_at desc, b.id desc")
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
        long total = ((Number) entityManager
                        .createNativeQuery("select count(*) from access_code_batch")
                        .getSingleResult())
                .longValue();
        return new Page<>(rows.stream().map(row -> summary((Object[]) row)).toList(), page, size, total);
    }

    @Override
    public Optional<BatchSummary> find(UUID batchId) {
        List<?> rows = entityManager
                .createNativeQuery(SUMMARIES + " where b.id = :batchId group by b.id")
                .setParameter(BATCH_ID, batchId)
                .getResultList();
        return rows.stream().findFirst().map(row -> summary((Object[]) row));
    }

    @Override
    public List<BatchSummary.Code> codesOf(UUID batchId) {
        List<?> rows = entityManager
                .createNativeQuery("select suffix, redemptions from access_code where batch_id = :batchId order by id")
                .setParameter(BATCH_ID, batchId)
                .getResultList();
        return rows.stream()
                .map(row -> (Object[]) row)
                .map(row -> new BatchSummary.Code((String) row[0], SqlValues.integer(row[1])))
                .toList();
    }

    @Override
    public boolean revoke(UUID batchId, Instant at) {
        return entityManager
                        .createNativeQuery("""
                                update access_code_batch set revoked_at = :at
                                where id = :batchId and revoked_at is null
                                """)
                        .setParameter(BATCH_ID, batchId)
                        .setParameter("at", at)
                        .executeUpdate()
                > 0;
    }

    @Override
    public Optional<RedeemableCode> find(String fingerprint) {
        return code(CODE_BY_FINGERPRINT, fingerprint);
    }

    @Override
    public Optional<RedeemableCode> lock(String fingerprint) {
        return code(CODE_BY_FINGERPRINT + " for update of c", fingerprint);
    }

    @Override
    public boolean hasRedeemed(UUID codeId, UUID userId) {
        return !entityManager
                .createNativeQuery("select 1 from access_code_redemption where code_id = :codeId and user_id = :userId")
                .setParameter("codeId", codeId)
                .setParameter("userId", userId)
                .getResultList()
                .isEmpty();
    }

    @Override
    public void redeem(RedeemableCode code, UUID userId, Instant at) {
        entityManager
                .createNativeQuery("update access_code set redemptions = redemptions + 1 where id = :codeId")
                .setParameter("codeId", code.id())
                .executeUpdate();
        entityManager
                .createNativeQuery("""
                        insert into access_code_redemption (code_id, user_id, redeemed_at, plus_days)
                        values (:codeId, :userId, :at, :plusDays)
                        """)
                .setParameter("codeId", code.id())
                .setParameter("userId", userId)
                .setParameter("at", at)
                .setParameter("plusDays", code.batch().plusDays())
                .executeUpdate();
    }

    private Optional<RedeemableCode> code(String sql, String fingerprint) {
        List<?> rows = entityManager
                .createNativeQuery(sql)
                .setParameter("fingerprint", fingerprint)
                .getResultList();
        return rows.stream()
                .findFirst()
                .map(row -> (Object[]) row)
                .map(row -> new RedeemableCode((UUID) row[0], batch(row, 2), SqlValues.integer(row[1])));
    }

    private static BatchSummary summary(Object[] row) {
        return new BatchSummary(
                batch(row, 0),
                SqlValues.integer(row[BATCH_COLUMN_COUNT]),
                SqlValues.integer(row[BATCH_COLUMN_COUNT + 1]),
                SqlValues.integer(row[BATCH_COLUMN_COUNT + 2]));
    }

    /** Un lot lu dans {@code row} à partir de la colonne {@code offset}, dans l'ordre de {@link #BATCH_COLUMNS}. */
    private static AccessCodeBatch batch(Object[] row, int offset) {
        return new AccessCodeBatch(
                (UUID) row[offset],
                (String) row[offset + 1],
                SqlValues.integer(row[offset + 2]),
                SqlValues.integer(row[offset + 3]),
                SqlValues.nullableInstant(row[offset + 4]),
                SqlValues.instant(row[offset + 5]),
                SqlValues.nullableInstant(row[offset + 6]));
    }
}
