package com.recallio.entitlement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CodeAttemptsTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    @Test
    void shouldLetTenFailuresThroughAndBlockTheEleventhAttempt() {
        CodeAttempts attempts = CodeAttempts.first(NOW);
        for (int failure = 2; failure <= CodeAttempts.MAX_FAILURES; failure++) {
            assertThat(attempts.isBlockedAt(NOW))
                    .as("après %d essais ratés", failure - 1)
                    .isFalse();
            attempts = attempts.afterFailureAt(NOW.plusSeconds(failure));
        }

        assertThat(attempts.failures()).isEqualTo(10);
        assertThat(attempts.windowStartedAt()).isEqualTo(NOW);
        assertThat(attempts.isBlockedAt(NOW.plusSeconds(30))).isTrue();
    }

    @Test
    void shouldUnblockWhenTheHourIsOverByTheClock() {
        var exhausted = new CodeAttempts(NOW, CodeAttempts.MAX_FAILURES);

        assertThat(exhausted.isBlockedAt(NOW.plus(CodeAttempts.WINDOW).minusSeconds(1)))
                .isTrue();
        assertThat(exhausted.isBlockedAt(NOW.plus(CodeAttempts.WINDOW))).isFalse();
    }

    @Test
    void shouldOpenANewWindowWithTheFirstFailureAfterTheHour() {
        var old = new CodeAttempts(NOW, 7);
        Instant later = NOW.plus(Duration.ofHours(3));

        assertThat(old.afterFailureAt(later)).isEqualTo(new CodeAttempts(later, 1));
        assertThat(old.afterFailureAt(NOW.plusSeconds(10))).isEqualTo(new CodeAttempts(NOW, 8));
    }
}
