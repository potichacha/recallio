package com.recallio.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PlusEntitlementTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldBeActiveUntilItsEndAndNotAfter() {
        var entitlement = new PlusEntitlement(NOW.plusSeconds(1));

        assertThat(entitlement.isActiveAt(CLOCK.instant())).isTrue();
        assertThat(entitlement.isActiveAt(NOW.plusSeconds(1))).isFalse();
        assertThat(entitlement.isActiveAt(NOW.plus(Duration.ofDays(1)))).isFalse();
    }

    @Test
    void shouldStartFromNowWhenThereIsNoPlusLeft() {
        var lapsed = new PlusEntitlement(NOW.minus(Duration.ofDays(40)));

        assertThat(lapsed.extendedBy(30, NOW).endsAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        // Un droit qui finit à l'instant même est échu : c'est celui d'un compte qui n'a jamais eu Plus.
        assertThat(new PlusEntitlement(NOW).extendedBy(7, NOW).endsAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
    }

    @Test
    void shouldAddTheDaysToWhatIsLeft() {
        var running = new PlusEntitlement(NOW.plus(Duration.ofDays(12)));

        PlusEntitlement extended = running.extendedBy(30, NOW);

        assertThat(extended.endsAt()).isEqualTo(NOW.plus(Duration.ofDays(42)));
        assertThat(extended.extendedBy(1, NOW).endsAt()).isEqualTo(NOW.plus(Duration.ofDays(43)));
    }

    @Test
    void shouldRefuseToExtendByLessThanADay() {
        var entitlement = new PlusEntitlement(NOW);

        assertThatThrownBy(() -> entitlement.extendedBy(0, NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> entitlement.extendedBy(-3, NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCountTheDaysLeftAStartedDayCountingForOne() {
        assertThat(new PlusEntitlement(NOW.plus(Duration.ofDays(30))).daysLeftAt(NOW))
                .isEqualTo(30);
        assertThat(new PlusEntitlement(NOW.plus(Duration.ofDays(30)).minusSeconds(1)).daysLeftAt(NOW))
                .isEqualTo(30);
        assertThat(new PlusEntitlement(NOW.plus(Duration.ofDays(30)).plusSeconds(1)).daysLeftAt(NOW))
                .isEqualTo(31);
        assertThat(new PlusEntitlement(NOW.plusMillis(1)).daysLeftAt(NOW)).isEqualTo(1);
        assertThat(new PlusEntitlement(NOW).daysLeftAt(NOW)).isZero();
        assertThat(new PlusEntitlement(NOW.minus(Duration.ofDays(2))).daysLeftAt(NOW))
                .isZero();
    }
}
