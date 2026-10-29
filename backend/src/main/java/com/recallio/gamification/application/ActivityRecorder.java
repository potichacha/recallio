package com.recallio.gamification.application;

import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.PointsLedger;
import com.recallio.gamification.domain.PointsLedger.PointsEntry;
import com.recallio.gamification.domain.ScoringRules;
import com.recallio.gamification.domain.ScoringRules.PointsAward;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.CardReviewed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.LocalDate;

/**
 * Réagit aux révisions du module review (ADR 0012) : compte la carte dans la journée de l'utilisateur et attribue les
 * points. Observateur synchrone, exécuté dans la transaction de la révision : soit tout est enregistré, soit rien.
 */
@ApplicationScoped
public class ActivityRecorder {

    private final ActivityRepository activity;
    private final PointsLedger ledger;
    private final ScoringRules rules;
    private final IdGenerator idGenerator;

    public ActivityRecorder(
            ActivityRepository activity, PointsLedger ledger, GamificationConfig config, IdGenerator idGenerator) {
        this.activity = activity;
        this.ledger = ledger;
        this.rules = config.rules();
        this.idGenerator = idGenerator;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    void onCardReviewed(@Observes CardReviewed event) {
        // « Le jour » est celui du fuseau de l'utilisateur, pas celui du serveur (CLAUDE.md, section 4.3).
        LocalDate day = LocalDate.ofInstant(event.reviewedAt(), event.user().timezone());
        int cardsToday = activity.recordReview(event.user().id(), day, rules.dailyGoal());
        for (PointsAward award : rules.awardsFor(cardsToday)) {
            ledger.append(new PointsEntry(
                    idGenerator.newId(),
                    event.user().id(),
                    award.amount(),
                    award.reason(),
                    event.reviewId(),
                    event.reviewedAt()));
        }
    }
}
