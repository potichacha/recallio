package com.recallio.gamification.application;

import com.recallio.gamification.domain.leaderboard.Week;
import com.recallio.gamification.domain.leaderboard.WeeklyActivity;
import com.recallio.gamification.domain.leaderboard.WeeklyRanking;
import com.recallio.gamification.domain.leaderboard.WeeklyRanking.Standing;
import com.recallio.group.GroupLeaderboards;
import com.recallio.group.GroupLeaderboards.LeaderboardSetup;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Le classement de la semaine d'un groupe (CLAUDE.md, section 6.4 ; ADR 0018). Le module group dit qui en est membre et
 * comment l'afficher ; ce module apporte l'activité et l'XP de chacun.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class GroupLeaderboardService {

    private final GroupLeaderboards groups;
    private final WeeklyActivity activity;
    private final Clock clock;

    public GroupLeaderboardService(GroupLeaderboards groups, WeeklyActivity activity, Clock clock) {
        this.groups = groups;
        this.activity = activity;
        this.clock = clock;
    }

    /**
     * La semaine en cours, dans le fuseau de celui qui consulte. Désactivé par le créateur : aucune ligne.
     *
     * @throws com.recallio.platform.domain.ResourceNotFoundException si l'utilisateur n'est pas membre du groupe
     */
    public Leaderboard currentWeek(UserRef viewer, UUID groupId) {
        LeaderboardSetup setup = groups.setup(viewer, groupId);
        Week week = Week.containing(LocalDate.now(clock.withZone(viewer.timezone())));
        if (setup.visibility() == LeaderboardVisibility.HIDDEN) {
            return new Leaderboard(week, setup.dailyGoal(), setup.visibility(), List.of());
        }
        Map<UUID, String> names = setup.members().stream()
                .collect(Collectors.toMap(GroupLeaderboards.Member::userId, GroupLeaderboards.Member::displayName));
        var weeks = activity.of(names.keySet(), week, viewer.timezone(), setup.dailyGoal());
        boolean anonymous = setup.visibility() == LeaderboardVisibility.ANONYMOUS;
        List<Entry> entries = WeeklyRanking.rank(weeks).stream()
                .map(standing -> entry(standing, viewer, names, anonymous))
                .toList();
        return new Leaderboard(week, setup.dailyGoal(), setup.visibility(), entries);
    }

    /** Chacun voit son propre nom ; ceux des autres seulement dans un classement nominatif. */
    private static Entry entry(Standing standing, UserRef viewer, Map<UUID, String> names, boolean anonymous) {
        var member = standing.member();
        boolean me = member.userId().equals(viewer.id());
        return new Entry(
                standing.rank(),
                me || !anonymous ? names.get(member.userId()) : null,
                me,
                member.regularDays(),
                member.xp(),
                member.cardsReviewed());
    }

    public record Leaderboard(Week week, int dailyGoal, LeaderboardVisibility visibility, List<Entry> entries) {}

    /**
     * @param displayName le nom du membre ; absent dans un classement anonymisé, sauf pour soi-même ({@code me})
     */
    public record Entry(
            int rank, @Nullable String displayName, boolean me, int regularDays, long xp, int cardsReviewed) {}
}
