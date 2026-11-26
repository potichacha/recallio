package com.recallio.entitlement.infrastructure;

import com.recallio.entitlement.domain.PlusEntitlement;
import com.recallio.entitlement.domain.PlusEntitlementRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Un module simple, aux requêtes natives : une ligne par compte, sans entité (CLAUDE.md, section 4.3). */
@ApplicationScoped
public class JpaPlusEntitlementRepository implements PlusEntitlementRepository {

    private static final String USER_ID = "userId";

    private final EntityManager entityManager;

    public JpaPlusEntitlementRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<PlusEntitlement> find(UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery("select ends_at from plus_entitlement where user_id = :userId")
                .setParameter(USER_ID, userId)
                .getResultList();
        return rows.stream().findFirst().map(endsAt -> new PlusEntitlement(SqlValues.instant(endsAt)));
    }

    @Override
    public PlusEntitlement lock(UUID userId, Instant now) {
        // La ligne doit exister pour être verrouillée : un droit qui finit maintenant est un droit échu.
        entityManager
                .createNativeQuery("""
                        insert into plus_entitlement (user_id, ends_at, updated_at)
                        values (:userId, :now, :now)
                        on conflict (user_id) do nothing
                        """)
                .setParameter(USER_ID, userId)
                .setParameter("now", now)
                .executeUpdate();
        Object endsAt = entityManager
                .createNativeQuery("select ends_at from plus_entitlement where user_id = :userId for update")
                .setParameter(USER_ID, userId)
                .getSingleResult();
        return new PlusEntitlement(SqlValues.instant(endsAt));
    }

    @Override
    public void save(UUID userId, PlusEntitlement entitlement, Instant now) {
        entityManager
                .createNativeQuery("""
                        update plus_entitlement set ends_at = :endsAt, updated_at = :now
                        where user_id = :userId
                        """)
                .setParameter(USER_ID, userId)
                .setParameter("endsAt", entitlement.endsAt())
                .setParameter("now", now)
                .executeUpdate();
    }
}
