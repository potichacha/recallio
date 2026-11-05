package com.recallio.ranked.infrastructure;

import static com.recallio.ranked.infrastructure.SqlValues.number;

import com.recallio.ranked.domain.RankedRepositories.CardRatingRepository;
import com.recallio.ranked.domain.glicko.Rating;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JpaCardRatingRepository implements CardRatingRepository {

    private final EntityManager entityManager;

    public JpaCardRatingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Map<UUID, Rating> ratings(Collection<UUID> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        List<?> rows = entityManager
                .createNativeQuery("""
                        select card_id, rating, deviation, volatility from card_rating
                        where card_id = any (cast(:ids as uuid[]))
                        """)
                .setParameter("ids", cardIds.toArray(UUID[]::new))
                .getResultList();
        Map<UUID, Rating> ratings = new HashMap<>();
        for (Object row : rows) {
            Object[] columns = (Object[]) row;
            ratings.put((UUID) columns[0], new Rating(number(columns[1]), number(columns[2]), number(columns[3])));
        }
        return ratings;
    }

    @Override
    public Rating lock(UUID cardId, String subjectSlug, Instant now) {
        entityManager
                .createNativeQuery("""
                        insert into card_rating (card_id, subject_slug, rating, deviation, volatility, answers, updated_at)
                        values (:cardId, :subject, :rating, :deviation, :volatility, 0, :now)
                        on conflict (card_id) do nothing
                        """)
                .setParameter("cardId", cardId)
                .setParameter("subject", subjectSlug)
                .setParameter("rating", Rating.START.rating())
                .setParameter("deviation", Rating.START.deviation())
                .setParameter("volatility", Rating.START.volatility())
                .setParameter("now", now)
                .executeUpdate();
        Object[] columns = (Object[]) entityManager
                .createNativeQuery(
                        "select rating, deviation, volatility from card_rating where card_id = :cardId for update")
                .setParameter("cardId", cardId)
                .getSingleResult();
        return new Rating(number(columns[0]), number(columns[1]), number(columns[2]));
    }

    @Override
    public void save(UUID cardId, Rating rating, Instant now) {
        entityManager
                .createNativeQuery("""
                        update card_rating
                        set rating = :rating, deviation = :deviation, volatility = :volatility, answers = answers + 1,
                            updated_at = :now
                        where card_id = :cardId
                        """)
                .setParameter("rating", rating.rating())
                .setParameter("deviation", rating.deviation())
                .setParameter("volatility", rating.volatility())
                .setParameter("now", now)
                .setParameter("cardId", cardId)
                .executeUpdate();
    }
}
