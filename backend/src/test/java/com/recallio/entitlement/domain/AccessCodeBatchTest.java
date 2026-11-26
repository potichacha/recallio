package com.recallio.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/** Un lot, et ce qui fait qu'un de ses codes marche encore. */
class AccessCodeBatchTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    @Test
    void shouldStayOpenWithoutAnExpiryDate() {
        AccessCodeBatch batch = batch(1, null, null);

        assertThat(batch.isExpiredAt(NOW.plus(Duration.ofDays(3650)))).isFalse();
        assertThat(batch.isOpenAt(NOW)).isTrue();
    }

    @Test
    void shouldExpireAtItsDateByTheClock() {
        AccessCodeBatch batch = batch(1, NOW.plusSeconds(60), null);

        assertThat(batch.isOpenAt(NOW)).isTrue();
        assertThat(batch.isExpiredAt(NOW.plusSeconds(59))).isFalse();
        assertThat(batch.isExpiredAt(NOW.plusSeconds(60))).isTrue();
        assertThat(batch.isOpenAt(NOW.plusSeconds(61))).isFalse();
    }

    @Test
    void shouldCloseWhenRevoked() {
        AccessCodeBatch batch = batch(1, null, NOW.minusSeconds(1));

        assertThat(batch.isRevoked()).isTrue();
        assertThat(batch.isOpenAt(NOW)).isFalse();
    }

    @Test
    void shouldLetACodeBeRedeemedUntilItsLastUse() {
        AccessCodeBatch threeUses = batch(3, null, null);

        assertThat(code(threeUses, 0).isRedeemableAt(NOW)).isTrue();
        assertThat(code(threeUses, 2).isRedeemableAt(NOW)).isTrue();
        assertThat(code(threeUses, 3).isRedeemableAt(NOW)).isFalse();
        assertThat(code(batch(1, null, null), 1).isRedeemableAt(NOW)).isFalse();
    }

    @Test
    void shouldNotLetACodeOfAClosedBatchBeRedeemed() {
        assertThat(code(batch(3, NOW, null), 0).isRedeemableAt(NOW)).isFalse();
        assertThat(code(batch(3, null, NOW), 0).isRedeemableAt(NOW)).isFalse();
    }

    @Test
    void shouldRefuseABatchOutOfBounds() {
        assertThatThrownBy(() -> new AccessCodeBatch(UUID.randomUUID(), " ", 30, 1, null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccessCodeBatch(
                        UUID.randomUUID(), "x".repeat(AccessCodeBatch.MAX_LABEL_LENGTH + 1), 30, 1, null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccessCodeBatch(UUID.randomUUID(), "Bêta", 0, 1, null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccessCodeBatch(
                        UUID.randomUUID(), "Bêta", AccessCodeBatch.MAX_PLUS_DAYS + 1, 1, null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AccessCodeBatch(UUID.randomUUID(), "Bêta", 30, 0, null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static AccessCodeBatch batch(int maxRedemptions, @Nullable Instant expiresAt, @Nullable Instant revokedAt) {
        return new AccessCodeBatch(
                UUID.randomUUID(), "Bêta", 30, maxRedemptions, expiresAt, NOW.minus(Duration.ofDays(1)), revokedAt);
    }

    private static RedeemableCode code(AccessCodeBatch batch, int redemptions) {
        return new RedeemableCode(UUID.randomUUID(), batch, redemptions);
    }
}
