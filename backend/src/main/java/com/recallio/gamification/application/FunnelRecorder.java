package com.recallio.gamification.application;

import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.funnel.FunnelEventRepository;
import com.recallio.gamification.domain.funnel.FunnelStep;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.CardReviewed;
import com.recallio.review.TrialStarted;
import com.recallio.user.AccountCreated;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;

/**
 * Enregistre le parcours des visiteurs (ADR 0064) : la visite que la page publique annonce, et les étapes que publient
 * les modules review et user. Un événement ne garde que son étape et son instant, jamais qui l'a franchie.
 */
@ApplicationScoped
public class FunnelRecorder {

    private final FunnelEventRepository events;
    private final ActivityRepository activity;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public FunnelRecorder(
            FunnelEventRepository events, ActivityRepository activity, IdGenerator idGenerator, Clock clock) {
        this.events = events;
        this.activity = activity;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /** Une visite de la page publique, que le navigateur annonce une fois par session. */
    @Transactional
    public void recordVisit() {
        record(FunnelStep.VISIT, clock.instant());
    }

    /** L'essai n'enregistre rien et publie hors de toute transaction : l'événement a la sienne. */
    @Transactional
    void onTrialStarted(@Observes TrialStarted event) {
        record(FunnelStep.TRIAL, event.startedAt());
    }

    @Transactional(Transactional.TxType.MANDATORY)
    void onAccountCreated(@Observes AccountCreated event) {
        record(FunnelStep.ACCOUNT, event.createdAt());
    }

    /**
     * La toute première révision : après elle, l'activité de l'utilisateur compte une seule carte. Passe après
     * {@link ActivityRecorder}, qui vient de la compter dans la même transaction ; deux premières révisions simultanées
     * se suivent sur la ligne du jour, et seule la première trouve une carte.
     */
    @Transactional(Transactional.TxType.MANDATORY)
    void onCardReviewed(@Observes @Priority(Interceptor.Priority.APPLICATION + 600) CardReviewed event) {
        if (activity.totalReviews(event.user().id()) == 1) {
            record(FunnelStep.FIRST_REVIEW, event.reviewedAt());
        }
    }

    private void record(FunnelStep step, Instant occurredAt) {
        events.record(idGenerator.newId(), step, occurredAt);
    }
}
