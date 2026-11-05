package com.recallio.ranked.application;

import com.recallio.ranked.domain.Duel;
import com.recallio.ranked.domain.Duel.DuelPlayer;
import com.recallio.ranked.domain.Duel.Status;
import com.recallio.ranked.domain.RankedRepositories.DuelRepository;
import com.recallio.ranked.domain.RankedRepositories.StandingRepository;
import com.recallio.ranked.domain.Season;
import com.recallio.ranked.domain.Standing;
import com.recallio.ranked.domain.glicko.Glicko2;
import com.recallio.ranked.domain.glicko.Glicko2.Game;
import com.recallio.ranked.domain.glicko.Rating;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Le règlement des duels (ADR 0046) : le résultat, puis la cote et la place de chaque joueur. Il se fait dès que les deux
 * joueurs ont fini, ou à la lecture d'un duel dont l'échéance est passée : aucune tâche planifiée.
 */
@ApplicationScoped
public class DuelResolution {

    private final DuelRepository duels;
    private final StandingRepository standings;

    public DuelResolution(DuelRepository duels, StandingRepository standings) {
        this.duels = duels;
        this.standings = standings;
    }

    /**
     * Règle le duel, verrouillé par l'appelant, s'il doit l'être : sans adversaire à l'échéance, il expire ; complet, ce
     * qui n'a pas été joué à temps est perdu ; les deux joueurs ayant fini, il est réglé.
     *
     * @return le duel tel qu'il est maintenant
     */
    @Transactional(Transactional.TxType.MANDATORY)
    public Duel settleIfDue(Duel duel, Instant now) {
        if (!duel.isPlayable()) {
            return duel;
        }
        if (duel.isPastDeadline(now)) {
            if (duel.players().size() < 2) {
                duels.settle(duel.id(), Status.EXPIRED, Map.of(), now);
                return reload(duel);
            }
            duel.players().stream()
                    .filter(player -> !player.hasFinished())
                    .forEach(player -> duels.finish(duel.id(), player.userId(), now));
            return resolve(reload(duel), now);
        }
        boolean bothFinished =
                duel.players().size() == 2 && duel.players().stream().allMatch(DuelPlayer::hasFinished);
        return bothFinished ? resolve(duel, now) : duel;
    }

    private Duel resolve(Duel duel, Instant now) {
        DuelPlayer first = duel.players().get(0);
        DuelPlayer second = duel.players().get(1);
        int questions = duel.cardIds().size();
        double firstScore = first.score(questions).against(second.score(questions));
        Map<UUID, Double> after = new HashMap<>();
        after.put(first.userId(), rate(first, second.ratingBefore(), firstScore, duel.subjectSlug(), now));
        after.put(second.userId(), rate(second, first.ratingBefore(), Game.WIN - firstScore, duel.subjectSlug(), now));
        duels.settle(duel.id(), Status.FINISHED, after, now);
        return reload(duel);
    }

    /** La nouvelle cote du joueur, dans la saison en cours : une nouvelle saison garde d'abord l'insigne de l'ancienne. */
    private double rate(DuelPlayer player, Rating opponent, double score, String subjectSlug, Instant now) {
        Season season = Season.of(now);
        Standing stored = standings
                .lock(player.userId(), subjectSlug)
                .orElseGet(() -> Standing.start(player.userId(), subjectSlug, season));
        if (!stored.season().equals(season)) {
            stored.peakTier()
                    .ifPresent(tier -> standings.award(player.userId(), subjectSlug, stored.season(), tier, now));
        }
        Standing current = stored.inSeason(season);
        Rating rating = Glicko2.update(current.rating(), List.of(new Game(opponent, score)));
        standings.save(current.after(rating, score), now);
        return rating.rating();
    }

    private Duel reload(Duel duel) {
        return duels.find(duel.id()).orElseThrow();
    }
}
