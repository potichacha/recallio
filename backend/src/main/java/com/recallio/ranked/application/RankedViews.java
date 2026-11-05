package com.recallio.ranked.application;

import com.recallio.ranked.RankedProfiles;
import com.recallio.ranked.domain.Duel;
import com.recallio.ranked.domain.Duel.DuelPlayer;
import com.recallio.ranked.domain.Duel.Status;
import com.recallio.ranked.domain.DuelScore;
import com.recallio.ranked.domain.Rank;
import com.recallio.ranked.domain.RankedRepositories.Badge;
import com.recallio.ranked.domain.RankedRepositories.DuelRepository;
import com.recallio.ranked.domain.RankedRepositories.StandingRepository;
import com.recallio.ranked.domain.Season;
import com.recallio.ranked.domain.Standing;
import com.recallio.user.FollowedSubjects;
import com.recallio.user.UserDirectory;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Cas d'usage : ce que l'utilisateur voit du classé (ADR 0046) — son rang par sujet, ses insignes, ses duels — sans
 * jamais de cote : seulement des rangs.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class RankedViews implements RankedProfiles {

    static final int RECENT_DUELS = 20;

    private final StandingRepository standings;
    private final DuelRepository duels;
    private final FollowedSubjects followedSubjects;
    private final UserDirectory users;
    private final Clock clock;

    public RankedViews(
            StandingRepository standings,
            DuelRepository duels,
            FollowedSubjects followedSubjects,
            UserDirectory users,
            Clock clock) {
        this.standings = standings;
        this.duels = duels;
        this.followedSubjects = followedSubjects;
        this.users = users;
        this.clock = clock;
    }

    /** Une place par sujet suivi, puis par sujet où l'utilisateur a joué sans le suivre, dans la saison en cours. */
    public Overview overview(UserRef user) {
        Season season = Season.of(clock.instant());
        Map<String, Standing> mine = new LinkedHashMap<>();
        standings.of(user.id()).forEach(standing -> mine.put(standing.subjectSlug(), standing.inSeason(season)));
        List<Standing> rows = new ArrayList<>();
        for (String subject : followedSubjects.of(user)) {
            Standing standing = mine.remove(subject);
            rows.add(standing != null ? standing : Standing.start(user.id(), subject, season));
        }
        rows.addAll(mine.values());
        return new Overview(season, rows, standings.badges(user.id()));
    }

    @Override
    public Map<String, PublicRank> ranks(UUID userId) {
        Season season = Season.of(clock.instant());
        Map<String, PublicRank> ranks = new LinkedHashMap<>();
        for (Standing standing : standings.of(userId)) {
            standing.inSeason(season)
                    .rank()
                    .ifPresent(rank -> ranks.put(
                            standing.subjectSlug(), new PublicRank(rank.tier().name(), rank.division())));
        }
        return ranks;
    }

    /** Les derniers duels de l'utilisateur, les plus récents d'abord. */
    public List<DuelView> recent(UserRef user) {
        return duels.recent(user.id(), RECENT_DUELS).stream()
                .map(duel -> view(duel, user))
                .toList();
    }

    /** Le duel vu par l'un de ses joueurs : l'adversaire, et le résultat une fois réglé. */
    public DuelView view(Duel duel, UserRef user) {
        DuelPlayer me = duel.player(user.id()).orElseThrow();
        Optional<DuelPlayer> opponent = duel.opponentOf(user.id());
        @Nullable
        String opponentName = opponent.map(
                        player -> users.displayNames(List.of(player.userId())).get(player.userId()))
                .orElse(null);
        @Nullable
        Result result =
                duel.status() == Status.FINISHED && opponent.isPresent() ? result(duel, me, opponent.get()) : null;
        return new DuelView(duel, me, opponent.orElse(null), opponentName, result);
    }

    private Result result(Duel duel, DuelPlayer me, DuelPlayer opponent) {
        int questions = duel.cardIds().size();
        DuelScore mine = me.score(questions);
        DuelScore theirs = opponent.score(questions);
        boolean placing = standings
                .find(me.userId(), duel.subjectSlug())
                .map(Standing::isPlacing)
                .orElse(true);
        Rank before = Rank.of(me.ratingBefore().rating());
        Double ratingAfter = me.ratingAfter();
        @Nullable Rank after = ratingAfter == null ? null : Rank.of(ratingAfter);
        boolean promoted = !placing && after != null && after.tier().compareTo(before.tier()) > 0;
        return new Result(
                mine.against(theirs), mine, theirs, placing ? null : before, placing ? null : after, placing, promoted);
    }

    public record Overview(Season season, List<Standing> standings, List<Badge> badges) {}

    /**
     * @param opponent absent tant que personne n'a rejoint le duel
     * @param opponentName son nom affiché, s'il est connu
     * @param result absent tant que le duel n'est pas réglé
     */
    public record DuelView(
            Duel duel,
            DuelPlayer me,
            @Nullable DuelPlayer opponent,
            @Nullable String opponentName,
            @Nullable Result result) {}

    /**
     * @param score celui du joueur : 1 gagné, 0,5 nul, 0 perdu
     * @param before son rang avant le duel ; absent pendant le placement
     * @param after son rang après ; absent pendant le placement
     * @param promoted vrai s'il est monté d'un rang (pas seulement d'une division)
     */
    public record Result(
            double score,
            DuelScore mine,
            DuelScore theirs,
            @Nullable Rank before,
            @Nullable Rank after,
            boolean placing,
            boolean promoted) {}
}
