package com.recallio.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.group.LeaderboardVisibility;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GroupSettingsTest {

    @Test
    void shouldStartWithoutSubjectsAndWithEveryFormat() {
        assertThat(GroupSettings.DEFAULTS.subjectSlugs()).isEmpty();
        assertThat(GroupSettings.DEFAULTS.cardTypes()).isEmpty();
        assertThat(GroupSettings.DEFAULTS.dailyGoal()).isEqualTo(GroupSettings.DEFAULT_DAILY_GOAL);
        assertThat(GroupSettings.DEFAULTS.leaderboardVisibility()).isEqualTo(LeaderboardVisibility.NAMED);
    }

    @Test
    void shouldKeepTheChosenOrderWithoutDuplicates() {
        var settings = new GroupSettings(
                List.of("go", "java", "go"), List.of("go-a", "go-a"), Set.of(), 10, LeaderboardVisibility.HIDDEN);

        assertThat(settings.subjectSlugs()).containsExactly("go", "java");
        assertThat(settings.themeSlugs()).containsExactly("go-a");
    }

    @Test
    void shouldBoundTheDailyGoalAndTheNumberOfSubjects() {
        assertThatThrownBy(() -> withGoal(GroupSettings.MIN_DAILY_GOAL - 1)).isInstanceOf(InvalidGroupException.class);
        assertThatThrownBy(() -> withGoal(GroupSettings.MAX_DAILY_GOAL + 1)).isInstanceOf(InvalidGroupException.class);
        assertThat(withGoal(GroupSettings.MAX_DAILY_GOAL).dailyGoal()).isEqualTo(GroupSettings.MAX_DAILY_GOAL);

        var tooMany = IntStream.rangeClosed(0, GroupSettings.MAX_SUBJECTS)
                .mapToObj(i -> "sujet-" + i)
                .toList();
        assertThatThrownBy(() -> new GroupSettings(tooMany, List.of(), Set.of(), 10, LeaderboardVisibility.NAMED))
                .isInstanceOf(InvalidGroupException.class);
    }

    private static GroupSettings withGoal(int dailyGoal) {
        return new GroupSettings(List.of(), List.of(), Set.of(), dailyGoal, LeaderboardVisibility.NAMED);
    }
}
