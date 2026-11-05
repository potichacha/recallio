package com.recallio.ranked.infrastructure;

import static com.recallio.ranked.infrastructure.SqlValues.integer;
import static com.recallio.ranked.infrastructure.SqlValues.number;

import com.recallio.ranked.domain.Rank.Tier;
import com.recallio.ranked.domain.RankedRepositories.Badge;
import com.recallio.ranked.domain.RankedRepositories.StandingRepository;
import com.recallio.ranked.domain.Season;
import com.recallio.ranked.domain.Standing;
import com.recallio.ranked.domain.glicko.Rating;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JpaStandingRepository implements StandingRepository {

    private static final String COLUMNS = """
            select user_id, subject_slug, season, rating, deviation, volatility, season_duels, wins, losses, draws,
                   peak_rating
            from ranked_standing
            """;

    private final EntityManager entityManager;

    public JpaStandingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Standing> find(UUID userId, String subjectSlug) {
        return one(COLUMNS + " where user_id = :userId and subject_slug = :subject", userId, subjectSlug);
    }

    @Override
    public Optional<Standing> lock(UUID userId, String subjectSlug) {
        return one(COLUMNS + " where user_id = :userId and subject_slug = :subject for update", userId, subjectSlug);
    }

    @Override
    public List<Standing> of(UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery(COLUMNS + " where user_id = :userId order by subject_slug")
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream().map(row -> toDomain((Object[]) row)).toList();
    }

    @Override
    public void save(Standing standing, Instant now) {
        entityManager
                .createNativeQuery("""
                        insert into ranked_standing (user_id, subject_slug, season, rating, deviation, volatility,
                                                     season_duels, wins, losses, draws, peak_rating, updated_at)
                        values (:userId, :subject, :season, :rating, :deviation, :volatility, :duels, :wins, :losses,
                                :draws, cast(:peak as double precision), :now)
                        on conflict (user_id, subject_slug) do update
                        set season = excluded.season, rating = excluded.rating, deviation = excluded.deviation,
                            volatility = excluded.volatility, season_duels = excluded.season_duels,
                            wins = excluded.wins, losses = excluded.losses, draws = excluded.draws,
                            peak_rating = excluded.peak_rating, updated_at = excluded.updated_at
                        """)
                .setParameter("userId", standing.userId())
                .setParameter("subject", standing.subjectSlug())
                .setParameter("season", standing.season().id())
                .setParameter("rating", standing.rating().rating())
                .setParameter("deviation", standing.rating().deviation())
                .setParameter("volatility", standing.rating().volatility())
                .setParameter("duels", standing.seasonDuels())
                .setParameter("wins", standing.wins())
                .setParameter("losses", standing.losses())
                .setParameter("draws", standing.draws())
                .setParameter("peak", standing.peakRating())
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    public void award(UUID userId, String subjectSlug, Season season, Tier tier, Instant now) {
        entityManager
                .createNativeQuery("""
                        insert into ranked_badge (user_id, subject_slug, season, tier, awarded_at)
                        values (:userId, :subject, :season, :tier, :now)
                        on conflict (user_id, subject_slug, season) do nothing
                        """)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .setParameter("season", season.id())
                .setParameter("tier", tier.name())
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    public List<Badge> badges(UUID userId) {
        List<?> rows = entityManager
                .createNativeQuery("""
                        select subject_slug, season, tier from ranked_badge
                        where user_id = :userId order by season desc, subject_slug
                        """)
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream()
                .map(row -> {
                    Object[] columns = (Object[]) row;
                    return new Badge(
                            (String) columns[0], Season.parse((String) columns[1]), Tier.valueOf((String) columns[2]));
                })
                .toList();
    }

    private Optional<Standing> one(String sql, UUID userId, String subjectSlug) {
        List<?> rows = entityManager
                .createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("subject", subjectSlug)
                .getResultList();
        return rows.stream().findFirst().map(row -> toDomain((Object[]) row));
    }

    private static Standing toDomain(Object[] row) {
        return new Standing(
                (UUID) row[0],
                (String) row[1],
                Season.parse((String) row[2]),
                new Rating(number(row[3]), number(row[4]), number(row[5])),
                integer(row[6]),
                integer(row[7]),
                integer(row[8]),
                integer(row[9]),
                row[10] == null ? null : number(row[10]));
    }
}
