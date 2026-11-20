package com.recallio.review.domain.daily;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.NotionInfo.Level;
import com.recallio.review.domain.daily.DailyChallengeDraw.Candidate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DailyChallengeDrawTest {

    /** Une notion de ce niveau et de ce domaine, avec ce nombre de cartes. */
    private static List<Candidate> notion(int number, String domain, Level level, int cards) {
        UUID notion = new UUID(1, number);
        return IntStream.range(0, cards)
                .mapToObj(index -> new Candidate(new UUID(number, index), notion, domain, level))
                .toList();
    }

    private static List<Candidate> all(List<List<Candidate>> notions) {
        return notions.stream().flatMap(List::stream).toList();
    }

    private static final List<Candidate> SUBJECT = all(List.of(
            notion(1, "bases", Level.EXPERT, 2),
            notion(2, "bases", Level.BEGINNER, 3),
            notion(3, "outils", Level.BEGINNER, 2),
            notion(4, "bases", Level.BEGINNER, 2),
            notion(5, "outils", Level.INTERMEDIATE, 2),
            notion(6, "bases", Level.INTERMEDIATE, 2),
            notion(7, "objets", Level.INTERMEDIATE, 1),
            notion(8, "objets", Level.EXPERT, 1)));

    private static final Map<UUID, Candidate> BY_CARD =
            SUBJECT.stream().collect(Collectors.toMap(Candidate::cardId, Function.identity()));

    @Test
    void shouldAskTwoBeginnerTwoIntermediateThenOneExpertQuestion() {
        for (long seed = 0; seed < 20; seed++) {
            List<UUID> drawn = DailyChallengeDraw.draw(SUBJECT, Set.of(), seed);

            assertThat(levels(drawn))
                    .as("graine %d", seed)
                    .containsExactly(
                            Level.BEGINNER, Level.BEGINNER, Level.INTERMEDIATE, Level.INTERMEDIATE, Level.EXPERT);
        }
    }

    @Test
    void shouldAskOneQuestionPerNotionFromDifferentDomainsWhenPossible() {
        for (long seed = 0; seed < 20; seed++) {
            List<Candidate> drawn = candidates(DailyChallengeDraw.draw(SUBJECT, Set.of(), seed));

            assertThat(drawn).extracting(Candidate::notionKey).doesNotHaveDuplicates();
            // Deux débutantes : « outils » a la sienne, l'autre vient de « bases ». Deux intermédiaires : jamais deux
            // du même domaine, puisque chacun en a une.
            assertThat(drawn.subList(0, 2)).extracting(Candidate::domain).containsExactlyInAnyOrder("bases", "outils");
            assertThat(drawn.subList(2, 4)).extracting(Candidate::domain).doesNotHaveDuplicates();
        }
    }

    @Test
    void shouldDrawTheSameQuestionsFromTheSameSeed() {
        assertThat(DailyChallengeDraw.draw(SUBJECT, Set.of(), 7))
                .isEqualTo(DailyChallengeDraw.draw(SUBJECT, Set.of(), 7));
    }

    @Test
    void shouldNeverAskACardOfTheLastChallenges() {
        // Les cartes déjà posées : toutes sauf une par notion.
        Set<UUID> recent = SUBJECT.stream().collect(Collectors.groupingBy(Candidate::notionKey)).values().stream()
                .flatMap(cards -> cards.stream().skip(1))
                .map(Candidate::cardId)
                .collect(Collectors.toSet());

        for (long seed = 0; seed < 20; seed++) {
            assertThat(DailyChallengeDraw.draw(SUBJECT, recent, seed)).doesNotContainAnyElementsOf(recent);
        }
    }

    @Test
    void shouldAskRecentCardsAgainRatherThanNotHavingEnoughNotions() {
        Set<UUID> everything = SUBJECT.stream().map(Candidate::cardId).collect(Collectors.toSet());

        assertThat(DailyChallengeDraw.draw(SUBJECT, everything, 3)).hasSize(DailyChallenge.QUESTIONS);
    }

    @Test
    void shouldCompleteAMissingLevelWithTheClosestOneAndKeepTheOrder() {
        List<Candidate> noExpert = all(List.of(
                notion(11, "a", Level.BEGINNER, 1),
                notion(12, "b", Level.BEGINNER, 1),
                notion(13, "c", Level.INTERMEDIATE, 1),
                notion(14, "d", Level.INTERMEDIATE, 1),
                notion(15, "e", Level.INTERMEDIATE, 1)));
        List<Candidate> beginnersOnly = all(List.of(
                notion(21, "a", Level.BEGINNER, 1),
                notion(22, "a", Level.BEGINNER, 1),
                notion(23, "a", Level.BEGINNER, 1),
                notion(24, "a", Level.EXPERT, 1),
                notion(25, "a", Level.BEGINNER, 1)));

        assertThat(levelsOf(noExpert, DailyChallengeDraw.draw(noExpert, Set.of(), 1)))
                .containsExactly(
                        Level.BEGINNER, Level.BEGINNER, Level.INTERMEDIATE, Level.INTERMEDIATE, Level.INTERMEDIATE);
        assertThat(levelsOf(beginnersOnly, DailyChallengeDraw.draw(beginnersOnly, Set.of(), 1)))
                .containsExactly(Level.BEGINNER, Level.BEGINNER, Level.BEGINNER, Level.BEGINNER, Level.EXPERT);
    }

    @Test
    void shouldCountACardWithoutNotionAsItsOwnNotion() {
        List<Candidate> markdown = IntStream.range(0, 5)
                .mapToObj(index -> new Candidate(new UUID(2, index), null, null, Level.INTERMEDIATE))
                .toList();

        assertThat(DailyChallengeDraw.canDraw(markdown)).isTrue();
        assertThat(DailyChallengeDraw.draw(markdown, Set.of(), 1)).hasSize(DailyChallenge.QUESTIONS);
    }

    @Test
    void shouldRefuseTooFewNotions() {
        List<Candidate> four = all(List.of(
                notion(31, "a", Level.BEGINNER, 3),
                notion(32, "a", Level.BEGINNER, 3),
                notion(33, "a", Level.INTERMEDIATE, 3),
                notion(34, "a", Level.EXPERT, 3)));

        assertThat(DailyChallengeDraw.canDraw(four)).isFalse();
        assertThatThrownBy(() -> DailyChallengeDraw.draw(four, Set.of(), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Candidate> candidates(List<UUID> cardIds) {
        return cardIds.stream().map(BY_CARD::get).toList();
    }

    private static List<Level> levels(List<UUID> cardIds) {
        return candidates(cardIds).stream().map(Candidate::level).toList();
    }

    private static List<Level> levelsOf(List<Candidate> candidates, List<UUID> cardIds) {
        Map<UUID, Candidate> byCard =
                candidates.stream().collect(Collectors.toMap(Candidate::cardId, Function.identity()));
        return cardIds.stream().map(byCard::get).map(Candidate::level).toList();
    }
}
