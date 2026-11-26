package com.recallio.review.domain.drill;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** La limite du jour de la révision intensive, et l'empreinte qui rend sa correction idempotente (ADR 0079). */
class DrillQuotaTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 26);

    @Test
    void shouldCountWhatIsLeftOfTheDailyLimit() {
        assertThat(new DrillQuota(DrillUsage.none(DAY), 20).remaining()).isEqualTo(20);
        assertThat(new DrillQuota(new DrillUsage(DAY, 6, 4), 20).remaining()).isEqualTo(14);
        assertThat(new DrillQuota(new DrillUsage(DAY, 20, 4), 20).remaining()).isZero();
    }

    /** Un compte qui perd Plus en cours de journée a pu dépasser la limite : il ne lui reste rien, pas moins que rien. */
    @Test
    void shouldNeverLeaveLessThanNothing() {
        assertThat(new DrillQuota(new DrillUsage(DAY, 57, 40), 20).remaining()).isZero();
    }

    @Test
    void shouldHaveNoLimitWithPlus() {
        var quota = new DrillQuota(new DrillUsage(DAY, 57, 40), null);

        assertThat(quota.dailyLimit()).isNull();
        assertThat(quota.remaining()).isNull();
    }

    @Test
    void shouldGiveTheSameCheckIdToTheSameAnswerSentAgain() {
        UUID requestId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        assertThat(DrillCheckId.of(requestId, cardId, Set.of(0, 2)))
                .isEqualTo(DrillCheckId.of(requestId, cardId, Set.of(2, 0)));
    }

    /** Sinon un seul identifiant, réutilisé pour toutes les réponses, ferait passer sous la limite. */
    @Test
    void shouldGiveAnotherCheckIdToAnotherCardOrOtherChoices() {
        UUID requestId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID checkId = DrillCheckId.of(requestId, cardId, Set.of(0));

        assertThat(DrillCheckId.of(requestId, UUID.randomUUID(), Set.of(0))).isNotEqualTo(checkId);
        assertThat(DrillCheckId.of(requestId, cardId, Set.of(1))).isNotEqualTo(checkId);
        assertThat(DrillCheckId.of(requestId, cardId, Set.of())).isNotEqualTo(checkId);
        assertThat(DrillCheckId.of(UUID.randomUUID(), cardId, Set.of(0))).isNotEqualTo(checkId);
    }
}
