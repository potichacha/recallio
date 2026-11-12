package com.recallio.review.domain.track;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.NotionInfo;
import com.recallio.content.NotionInfo.Level;
import com.recallio.review.domain.track.LearningPath.NotionStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        assertThat(new LearningPath(NOTIONS, Map.of(VARIABLES, 4), Set.of()).acquired(VARIABLES))
                .isFalse();
        assertThat(new LearningPath(NOTIONS, Map.of(VARIABLES, 5), Set.of()).acquired(VARIABLES))
                .isTrue();
    }

    @Test
    void shouldAcquireASmallNotionOnceAllItsCardsAreRecalled() {
        assertThat(new LearningPath(NOTIONS, Map.of(TYPES, 2), Set.of()).acquired(TYPES))
                .isFalse();
        assertThat(new LearningPath(NOTIONS, Map.of(TYPES, 3), Set.of()).acquired(TYPES))
                .isTrue();
    }

    /** Sinon, une notion encore sans carte bloquerait tout ce qui en dépend. */
    @Test
    void shouldConsiderANotionWithoutCardsAcquired() {
        assertThat(new LearningPath(NOTIONS, Map.of(), Set.of()).acquired(EMPTY))
                .isTrue();
    }

    @Test
    void shouldUnlockANotionOnceEveryPrerequisiteIsAcquired() {
        var beginning = new LearningPath(NOTIONS, Map.of(), Set.of());
        var later = new LearningPath(NOTIONS, Map.of(VARIABLES, 5), Set.of());
        var end = new LearningPath(NOTIONS, Map.of(VARIABLES, 5, TYPES, 3), Set.of());

        assertThat(beginning.locked()).containsExactlyInAnyOrder(TYPES, GENERICS);
        assertThat(later.locked()).containsExactly(GENERICS);
        assertThat(end.locked()).isEmpty();
    }

    @Test
    void shouldGiveEachNotionItsStatusOnTheMap() {
        var path = new LearningPath(NOTIONS, Map.of(VARIABLES, 2), Set.of());

        assertThat(path.status(VARIABLES, 3, true)).isEqualTo(LearningPath.NotionStatus.IN_PROGRESS);
        assertThat(path.status(TYPES, 0, true)).isEqualTo(LearningPath.NotionStatus.LOCKED);
        assertThat(path.status(TYPES, 0, false)).isEqualTo(LearningPath.NotionStatus.AVAILABLE);
        assertThat(path.status(EMPTY, 0, true)).isEqualTo(LearningPath.NotionStatus.NO_CARDS);
        assertThat(new LearningPath(NOTIONS, Map.of(VARIABLES, 5), Set.of()).status(VARIABLES, 5, true))
                .isEqualTo(LearningPath.NotionStatus.ACQUIRED);
    }

    @Test
    void shouldNotLetAnUnknownPrerequisiteBlockANotion() {
        UUID retired = UUID.randomUUID();
        var path = new LearningPath(List.of(notion(TYPES, List.of(retired), 3)), Map.of(), Set.of());

        assertThat(path.unlocked(TYPES)).isTrue();
    }

    /** Qui apprend découvre une notion dans le Tour avant de la réviser (ADR 0059). */
    @Test
    void shouldMakeANotionOfTheTourWaitUntilItIsValidatedThere() {
        List<NotionInfo> notions = List.of(notion(VARIABLES, List.of(), 8, 2), notion(TYPES, List.of(), 3));
        var before = new LearningPath(notions, Map.of(), Set.of());
        var after = new LearningPath(notions, Map.of(), Set.of(VARIABLES));

        assertThat(before.awaitingTour()).containsExactly(VARIABLES);
        assertThat(before.status(VARIABLES, 0, true)).isEqualTo(NotionStatus.TOUR_FIRST);
        assertThat(after.awaitingTour()).isEmpty();
        assertThat(after.status(VARIABLES, 0, true)).isEqualTo(NotionStatus.AVAILABLE);
        // Une notion sans page dans le Tour n'attend rien.
        assertThat(before.status(TYPES, 0, true)).isEqualTo(NotionStatus.AVAILABLE);
    }

    @Test
    void shouldLetANotionAlreadyStartedGoOnWithoutTheTour() {
        var path = new LearningPath(List.of(notion(VARIABLES, List.of(), 8, 2)), Map.of(), Set.of());

        assertThat(path.status(VARIABLES, 1, true)).isEqualTo(NotionStatus.IN_PROGRESS);
    }

    @Test
    void shouldOnlyMakeTheTourComeFirstForWhoLearns() {
        var path = new LearningPath(List.of(notion(VARIABLES, List.of(), 8, 2)), Map.of(), Set.of());

        assertThat(path.status(VARIABLES, 0, false)).isEqualTo(NotionStatus.AVAILABLE);
    }

    @Test
    void shouldLockANotionOfTheTourOnItsPrerequisitesFirst() {
        List<NotionInfo> notions = List.of(notion(VARIABLES, List.of(), 8), notion(TYPES, List.of(VARIABLES), 3, 1));
        var path = new LearningPath(notions, Map.of(), Set.of());

        assertThat(path.status(TYPES, 0, true)).isEqualTo(NotionStatus.LOCKED);
    }

    private static NotionInfo notion(UUID id, List<UUID> requires, int cards) {
        return notion(id, requires, cards, 0);
    }

    private static NotionInfo notion(UUID id, List<UUID> requires, int cards, int tourPages) {
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
                tourPages);
    }
}
