package com.recallio.review.domain.track;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.NotionInfo;
import com.recallio.content.NotionInfo.Level;
import com.recallio.review.domain.queue.ReviewQueuePlanner.Candidate;
import com.recallio.user.LearningTrack;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class NewCardOrderTest {

    /** Dans l'ordre du programme : une notion experte essentielle, une débutante rare, une intermédiaire courante. */
    private static final NotionInfo EXPERT = notion(Level.EXPERT, 1);

    private static final NotionInfo BEGINNER = notion(Level.BEGINNER, 4);
    private static final NotionInfo INTERMEDIATE = notion(Level.INTERMEDIATE, 2);
    private static final Map<UUID, NotionInfo> NOTIONS = byId(EXPERT, BEGINNER, INTERMEDIATE);

    private final Candidate expert = card(EXPERT);
    private final Candidate beginner = card(BEGINNER);
    private final Candidate intermediate = card(INTERMEDIATE);
    private final List<Candidate> program = List.of(expert, beginner, intermediate);

    @Test
    void shouldOfferTheMostEssentialNotionsFirstToPrepareAnInterview() {
        assertThat(arrange(LearningTrack.PREPARE, Set.of())).containsExactly(expert, intermediate, beginner);
    }

    @Test
    void shouldGoFromBeginnerToExpertToLearn() {
        assertThat(arrange(LearningTrack.LEARN, Set.of())).containsExactly(beginner, intermediate, expert);
    }

    @Test
    void shouldKeepTheBeginnerNotionsForLastToMaintainALevel() {
        assertThat(arrange(LearningTrack.MAINTAIN, Set.of())).containsExactly(expert, intermediate, beginner);
    }

    @Test
    void shouldLeaveOutTheNewCardsOfALockedNotionOnlyToLearn() {
        Set<UUID> locked = Set.of(EXPERT.id());

        assertThat(arrange(LearningTrack.LEARN, locked)).containsExactly(beginner, intermediate);
        assertThat(arrange(LearningTrack.PREPARE, locked)).contains(expert);
    }

    /** Une carte déjà vue reste révisable, même si sa notion est de nouveau verrouillée (un prérequis oublié). */
    @Test
    void shouldKeepAKnownCardOfALockedNotion() {
        List<Candidate> arranged = NewCardOrder.arrange(
                program, Set.of(expert.cardId()), Map.of("java", LearningTrack.LEARN), NOTIONS, Set.of(EXPERT.id()));

        assertThat(arranged).contains(expert);
    }

    @Test
    void shouldKeepTheProgramOrderOfCardsWithoutNotionAndTheOrderOfSubjects() {
        Candidate markdown1 = new Candidate(UUID.randomUUID(), "go");
        Candidate markdown2 = new Candidate(UUID.randomUUID(), "go");
        List<Candidate> eligible = List.of(markdown1, expert, markdown2, beginner);

        assertThat(NewCardOrder.arrange(eligible, Set.of(), Map.of(), NOTIONS, Set.of()))
                .containsExactly(markdown1, markdown2, expert, beginner);
    }

    private List<Candidate> arrange(LearningTrack track, Set<UUID> locked) {
        return NewCardOrder.arrange(program, Set.of(), Map.of("java", track), NOTIONS, locked);
    }

    private static Candidate card(NotionInfo notion) {
        return new Candidate(UUID.randomUUID(), "java", notion.id());
    }

    private static NotionInfo notion(Level level, int priority) {
        return new NotionInfo(
                UUID.randomUUID(),
                "java",
                "notion",
                "Notion",
                "chapitre",
                "Chapitre",
                level,
                priority,
                List.of(),
                0,
                1);
    }

    private static Map<UUID, NotionInfo> byId(NotionInfo... notions) {
        return Stream.of(notions).collect(Collectors.toMap(NotionInfo::id, Function.identity()));
    }
}
