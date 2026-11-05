package com.recallio.gamification.application;

import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.RetentionReport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Cas d'usage : mesurer la rétention (ADR 0050), pour les admins. Le jour de chacun est celui de son fuseau, mais
 * « aujourd'hui » est pris en UTC : un écart d'un jour au bord de la fenêtre ne change pas la tendance.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class RetentionService {

    private final ActivityRepository activity;
    private final Clock clock;

    public RetentionService(ActivityRepository activity, Clock clock) {
        this.activity = activity;
        this.clock = clock;
    }

    public RetentionReport report() {
        return RetentionReport.of(activity.activeDays(), LocalDate.now(clock));
    }
}
