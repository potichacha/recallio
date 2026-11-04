package com.recallio.gamification.application;

import com.recallio.gamification.domain.ActivityRepository;
import com.recallio.gamification.domain.XpLedger;
import com.recallio.gamification.domain.XpLedger.XpEntry;
import com.recallio.gamification.domain.XpRules;
import com.recallio.gamification.domain.XpRules.ReviewedCard;
import com.recallio.gamification.domain.XpRules.XpAward;
import com.recallio.platform.domain.IdGenerator;
import com.recallio.review.CardReviewed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.LocalDate;

/**
 * Réagit aux révisions du module review (ADR 0012) : compte la carte dans la journée de l'utilisateur et attribue l'XP
 * (ADR 0035). Observateur synchrone, exécuté dans la transaction de la révision : soit tout est enregistré, soit rien.
 */
@ApplicationScoped
public class ActivityRecorder {

    private final ActivityRepository activity;
    private final XpLedger ledger;
    private final XpRules rules;
    private final IdGenerator idGenerator;

    public ActivityRecorder(
            ActivityRepository activity, XpLedger ledger, GamificationConfig config, IdGenerator idGenerator) {
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
        var card = new ReviewedCard(event.subjectSlug(), event.cardType(), event.recalled());
        for (XpAward award : rules.awardsFor(card, cardsToday)) {
            ledger.append(new XpEntry(
                    idGenerator.newId(),
                    event.user().id(),
                    award.amount(),
                    award.reason(),
                    award.subjectSlug(),
                    event.reviewId(),
                    event.reviewedAt()));
        }
    }
}
