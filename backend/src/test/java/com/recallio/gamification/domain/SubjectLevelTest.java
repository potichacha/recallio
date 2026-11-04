package com.recallio.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubjectLevelTest {

    @Test
    void shouldStartAtLevelOne() {
        assertThat(SubjectLevel.of(0)).isEqualTo(new SubjectLevel(1, 0, 0, 100));
    }

    @ParameterizedTest
    @CsvSource({"99, 1", "100, 2", "299, 2", "300, 3", "999, 4", "1000, 5", "4499, 9", "4500, 10", "19000, 20"})
    void shouldNeedOneHundredTimesTheLevelToReachTheNextOne(long xp, int level) {
        assertThat(SubjectLevel.of(xp).level()).isEqualTo(level);
    }

    @Test
    void shouldGiveTheBoundsOfTheCurrentLevel() {
        assertThat(SubjectLevel.of(340)).isEqualTo(new SubjectLevel(3, 340, 300, 600));
    }

    @Test
    void shouldRejectANegativeXp() {
        assertThatThrownBy(() -> SubjectLevel.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
