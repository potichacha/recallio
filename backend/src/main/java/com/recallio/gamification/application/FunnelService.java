package com.recallio.gamification.application;

import com.recallio.gamification.domain.funnel.FunnelEventRepository;
import com.recallio.gamification.domain.funnel.FunnelReport;
import com.recallio.gamification.domain.funnel.FunnelReport.FunnelWeek;
import com.recallio.gamification.domain.leaderboard.Week;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;

/** Cas d'usage : mesurer le parcours des visiteurs (ADR 0064), pour les admins. */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class FunnelService {

    private final FunnelEventRepository events;
    private final Clock clock;

    public FunnelService(FunnelEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    /** Les {@link FunnelReport#WEEKS} dernières semaines, la semaine en cours d'abord. */
    public FunnelReport report() {
        return new FunnelReport(FunnelReport.weeksUpTo(clock.instant()).stream()
                .map(this::measure)
                .toList());
    }

    private FunnelWeek measure(Week week) {
        return FunnelWeek.of(
                week, events.countsBetween(week.startIn(FunnelReport.ZONE), week.endIn(FunnelReport.ZONE)));
    }
}
