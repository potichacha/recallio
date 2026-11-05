package com.recallio.review.domain.track;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.NotionInfo;
import com.recallio.content.NotionInfo.Level;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LearningPathTest {

    private static final UUID VARIABLES = UUID.randomUUID();
    private static final UUID TYPES = UUID.randomUUID();
    private static final UUID GENERICS = UUID.randomUUID();
    private static final UUID EMPTY = UUID.randomUUID();

    /** Variables (8 cartes) → types (3 cartes) → generics, qui requiert aussi une notion encore sans carte. */
    private static final List<NotionInfo> NOTIONS = List.of(
            notion(VARIABLES, List.of(), 8),
            notion(TYPES, List.of(VARIABLES), 3),
            notion(EMPTY, List.of(), 0),
            notion(GENERICS, List.of(TYPES, EMPTY), 10));

    @Test
    void shouldAcquireANotionAfterFiveRecalledCards() {
        assertThat(new LearningPath(NOTIONS, Map.of(VARIABLES, 4)).acquired(VARIABLES))
                .isFalse();
        assertThat(new LearningPath(NOTIONS, Map.of(VARIABLES, 5)).acquired(VARIABLES))
                .isTrue();
    }

    @Test
    void shouldAcquireASmallNotionOnceAllItsCardsAreRecalled() {
        assertThat(new LearningPath(NOTIONS, Map.of(TYPES, 2)).acquired(TYPES)).isFalse();
        assertThat(new LearningPath(NOTIONS, Map.of(TYPES, 3)).acquired(TYPES)).isTrue();
    }

    /** Sinon, une notion encore sans carte bloquerait tout ce qui en dépend. */
    @Test
    void shouldConsiderANotionWithoutCardsAcquired() {
        assertThat(new LearningPath(NOTIONS, Map.of()).acquired(EMPTY)).isTrue();
    }

    @Test
    void shouldUnlockANotionOnceEveryPrerequisiteIsAcquired() {
        var beginning = new LearningPath(NOTIONS, Map.of());
        var later = new LearningPath(NOTIONS, Map.of(VARIABLES, 5));
        var end = new LearningPath(NOTIONS, Map.of(VARIABLES, 5, TYPES, 3));

        assertThat(beginning.locked()).containsExactlyInAnyOrder(TYPES, GENERICS);
        assertThat(later.locked()).containsExactly(GENERICS);
        assertThat(end.locked()).isEmpty();
    }

    @Test
    void shouldGiveEachNotionItsStatusOnTheMap() {
        var path = new LearningPath(NOTIONS, Map.of(VARIABLES, 2));

        assertThat(path.status(VARIABLES, 3, true)).isEqualTo(LearningPath.NotionStatus.IN_PROGRESS);
        assertThat(path.status(TYPES, 0, true)).isEqualTo(LearningPath.NotionStatus.LOCKED);
        assertThat(path.status(TYPES, 0, false)).isEqualTo(LearningPath.NotionStatus.AVAILABLE);
        assertThat(path.status(EMPTY, 0, true)).isEqualTo(LearningPath.NotionStatus.NO_CARDS);
        assertThat(new LearningPath(NOTIONS, Map.of(VARIABLES, 5)).status(VARIABLES, 5, true))
                .isEqualTo(LearningPath.NotionStatus.ACQUIRED);
    }

    @Test
    void shouldNotLetAnUnknownPrerequisiteBlockANotion() {
        UUID retired = UUID.randomUUID();
        var path = new LearningPath(List.of(notion(TYPES, List.of(retired), 3)), Map.of());

        assertThat(path.unlocked(TYPES)).isTrue();
    }

    private static NotionInfo notion(UUID id, List<UUID> requires, int cards) {
        return new NotionInfo(
                id,
                "java",
                "notion",
                "Notion",
                "domaine",
                "Domaine",
                "chapitre",
                "Chapitre",
                Level.BEGINNER,
                1,
                requires,
                0,
                cards,
                0);
    }
}
