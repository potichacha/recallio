package com.recallio.user.infrastructure;

import com.recallio.user.domain.DisplayName;
import com.recallio.user.domain.UserProfile;
import com.recallio.user.domain.UserProfileRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

@ApplicationScoped
public class JpaUserProfileRepository implements UserProfileRepository {

    private final EntityManager entityManager;
    private final Clock clock;

    public JpaUserProfileRepository(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public Optional<UserProfile> findBySubject(String oidcSubject) {
        return entityManager
                .createQuery("select u from AppUserEntity u where u.oidcSubject = :subject", AppUserEntity.class)
                .setParameter("subject", oidcSubject)
                .getResultStream()
                .findFirst()
                .map(JpaUserProfileRepository::toDomain);
    }

    /**
     * {@code ON CONFLICT DO NOTHING} rend la création atomique : si deux requêtes créent le même profil en même temps,
     * l'une insère, l'autre ne fait rien, et les deux relisent la même ligne.
     */
    @Override
    public UserProfile createIfAbsent(UserProfile candidate) {
        entityManager
                .createNativeQuery("""
                        insert into app_user (id, oidc_subject, display_name, timezone, created_at, updated_at)
                        values (:id, :subject, :displayName, :timezone, :createdAt, :createdAt)
                        on conflict (oidc_subject) do nothing
                        """)
                .setParameter("id", candidate.id())
                .setParameter("subject", candidate.oidcSubject())
                .setParameter("displayName", candidate.displayName().value())
                .setParameter("timezone", candidate.timezone().getId())
                .setParameter("createdAt", candidate.createdAt())
                .executeUpdate();
        return findBySubject(candidate.oidcSubject())
                .orElseThrow(() -> new IllegalStateException("Profil introuvable juste après sa création"));
    }

    @Override
    public void update(UserProfile profile) {
        AppUserEntity entity = entityManager.find(AppUserEntity.class, profile.id());
        if (entity == null) {
            throw new IllegalStateException("Profil introuvable : " + profile.id());
        }
        entity.displayName = profile.displayName().value();
        entity.timezone = profile.timezone().getId();
        entity.updatedAt = clock.instant();
    }

    private static UserProfile toDomain(AppUserEntity entity) {
        return new UserProfile(
                entity.id,
                entity.oidcSubject,
                new DisplayName(entity.displayName),
                ZoneId.of(entity.timezone),
                entity.createdAt);
    }
}
