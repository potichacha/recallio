package com.recallio.user.infrastructure;

import com.recallio.user.domain.PublicHandle;
import com.recallio.user.domain.PublicProfileRepository;
import com.recallio.user.domain.PublicProfileSettings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaPublicProfileRepository implements PublicProfileRepository {

    private final EntityManager entityManager;

    public JpaPublicProfileRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<PublicProfileSettings> findByUser(UUID userId) {
        return find("user_id = :key", userId);
    }

    @Override
    public Optional<PublicProfileSettings> findByHandle(PublicHandle handle) {
        return find("handle = :key", handle.value());
    }

    @Override
    public void save(PublicProfileSettings settings, Instant updatedAt) {
        entityManager
                .createNativeQuery("""
                        insert into public_profile (user_id, handle, visible, updated_at)
                        values (:userId, :handle, :visible, :at)
                        on conflict (user_id) do update
                        set handle = excluded.handle, visible = excluded.visible, updated_at = excluded.updated_at
                        """)
                .setParameter("userId", settings.userId())
                .setParameter("handle", settings.handle().value())
                .setParameter("visible", settings.visible())
                .setParameter("at", updatedAt)
                .executeUpdate();
    }

    private Optional<PublicProfileSettings> find(String condition, Object key) {
        List<?> rows = entityManager
                .createNativeQuery("select user_id, handle, visible from public_profile where " + condition)
                .setParameter("key", key)
                .getResultList();
        return rows.stream().findFirst().map(row -> {
            Object[] columns = (Object[]) row;
            return new PublicProfileSettings(
                    (UUID) columns[0], new PublicHandle((String) columns[1]), (Boolean) columns[2]);
        });
    }
}
