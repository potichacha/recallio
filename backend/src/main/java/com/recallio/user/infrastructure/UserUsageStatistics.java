package com.recallio.user.infrastructure;

import com.recallio.platform.domain.UsageFeature;
import com.recallio.platform.domain.UsageStatistics;
import com.recallio.platform.infrastructure.UsageStatisticsSql;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** L'usage vu par le module user (ADR 0076) : les comptes créés, les sujets suivis et les profils rendus publics. */
@ApplicationScoped
public class UserUsageStatistics implements UsageStatistics {

    private final UsageStatisticsSql sql;

    public UserUsageStatistics(UsageStatisticsSql sql) {
        this.sql = sql;
    }

    /** Un profil public a la date de son dernier réglage, pas celle où il a été consulté : il se compte à ce jour. */
    @Override
    public Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        return Map.of(UsageFeature.PUBLIC_PROFILE, sql.users("select user_id from public_profile where visible"));
    }

    @Override
    public Map<DailyCount, Map<LocalDate, Long>> dailyCounts(Instant from, Instant to) {
        return Map.of(DailyCount.ACCOUNTS_CREATED, sql.countsByDay("""
                select cast(created_at at time zone 'UTC' as date) as day, count(*)
                from app_user
                where created_at >= :from and created_at < :to
                group by day
                """, from, to));
    }

    @Override
    public Map<SubjectCount, Map<String, Long>> subjectCounts(Instant from, Instant to) {
        return Map.of(
                SubjectCount.FOLLOWERS,
                sql.countsByKey(
                        "select subject_slug, count(*) from followed_subject group by subject_slug", String.class));
    }
}
