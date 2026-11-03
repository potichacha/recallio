package com.recallio.user.infrastructure;

import com.recallio.user.LearningTrack;
import com.recallio.user.domain.FollowedSubjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Requêtes natives : une table d'association sans comportement ne mérite pas d'entité JPA. */
@ApplicationScoped
public class JpaFollowedSubjectRepository implements FollowedSubjectRepository {

    private final EntityManager entityManager;

    public JpaFollowedSubjectRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Set<String> findSlugs(UUID userId) {
        Set<String> slugs = new HashSet<>();
        entityManager
                .createNativeQuery("select subject_slug from followed_subject where user_id = :userId", String.class)
                .setParameter("userId", userId)
                .getResultList()
                .forEach(slug -> slugs.add((String) slug));
        return slugs;
    }

    @Override
    public Map<String, LearningTrack> findTracks(UUID userId) {
        Map<String, LearningTrack> tracks = new HashMap<>();
        entityManager
                .createNativeQuery(
                        "select subject_slug, track from followed_subject where user_id = :userId", Object[].class)
                .setParameter("userId", userId)
                .getResultList()
                .forEach(row -> {
                    Object[] columns = (Object[]) row;
                    tracks.put((String) columns[0], LearningTrack.valueOf((String) columns[1]));
                });
        return tracks;
    }

    @Override
    public boolean updateTrack(UUID userId, String slug, LearningTrack track) {
        return entityManager
                        .createNativeQuery(
                                "update followed_subject set track = :track where user_id = :userId and subject_slug = :slug")
                        .setParameter("track", track.name())
                        .setParameter("userId", userId)
                        .setParameter("slug", slug)
                        .executeUpdate()
                == 1;
    }

    @Override
    public void replace(UUID userId, Set<String> slugs, Instant now) {
        entityManager
                .createNativeQuery("""
                        delete from followed_subject
                        where user_id = :userId and not (subject_slug = any (cast(:slugs as text[])))
                        """)
                .setParameter("userId", userId)
                .setParameter("slugs", slugs.toArray(String[]::new))
                .executeUpdate();
        for (String slug : slugs) {
            entityManager
                    .createNativeQuery("""
                            insert into followed_subject (user_id, subject_slug, followed_at)
                            values (:userId, :slug, :now)
                            on conflict do nothing
                            """)
                    .setParameter("userId", userId)
                    .setParameter("slug", slug)
                    .setParameter("now", now)
                    .executeUpdate();
        }
    }
}
