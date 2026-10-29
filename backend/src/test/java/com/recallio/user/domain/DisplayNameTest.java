package com.recallio.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisplayNameTest {

    @Test
    void shouldStripSurroundingSpaces() {
        assertThat(new DisplayName("  Alice  ").value()).isEqualTo("Alice");
    }

    @Test
    void shouldRejectBlankOrTooLongNames() {
        assertThatThrownBy(() -> new DisplayName("   "))
                .isInstanceOf(InvalidProfileException.class)
                .extracting(e -> ((InvalidProfileException) e).code())
                .isEqualTo("user.invalid-display-name");
        assertThatThrownBy(() -> new DisplayName("x".repeat(DisplayName.MAX_LENGTH + 1)))
                .isInstanceOf(InvalidProfileException.class);
    }

    @Test
    void shouldFallBackToAGenericNameWhenTheProviderGivesNone() {
        assertThat(DisplayName.suggestedFrom(" ").value()).isEqualTo("Candidat");
    }

    @Test
    void shouldTruncateTooLongSuggestedNames() {
        assertThat(DisplayName.suggestedFrom("y".repeat(80)).value()).hasSize(DisplayName.MAX_LENGTH);
    }
}
