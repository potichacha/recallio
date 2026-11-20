package com.recallio.review.domain.level;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.NotionInfo;
import com.recallio.content.NotionInfo.Level;
import com.recallio.review.domain.level.LevelCheckDomain.Candidate;
import com.recallio.review.domain.level.LevelCheckExceptions.InvalidLevelCheckAnswersException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Le format du test de niveau (ADR 0068) : deux questions par domaine des bases, de notions différentes. */
class LevelCheckDomainTest {

    private static final int SEEDS = 50;

    private static final UUID VARIABLES = UUID.randomUUID();
    private static final UUID TYPES = UUID.randomUUID();
    private static final UUID LOOPS = UUID.randomUUID();

    @Test
    void shouldDrawTwoQuestionsOfDifferentNotionsFromDifferentChaptersWhenPossible() {
        // Deux notions du chapitre des variables, une seule de celui des boucles : chaque tirage en prend une.
        var domain = domain(
                candidates(VARIABLES, "variables", 3),
                candidates(TYPES, "variables", 2),
                candidates(LOOPS, "boucles", 1));

        IntStream.range(0, SEEDS).forEach(seed -> {
            List<Candidate> drawn = drawn(domain, seed);

            assertThat(drawn).hasSize(LevelCheckDomain.QUESTIONS_PER_DOMAIN);
            assertThat(drawn).extracting(Candidate::notionId).doesNotHaveDuplicates();
            assertThat(drawn).extracting(Candidate::chapter).containsExactlyInAnyOrder("variables", "boucles");
        });
    }

    @Test
    void shouldDrawTwoNotionsOfTheSameChapterWhenTheDomainHasOnlyOne() {
        var domain = domain(candidates(VARIABLES, "variables", 3), candidates(TYPES, "variables", 1));

        IntStream.range(0, SEEDS)
                .forEach(seed -> assertThat(drawn(domain, seed))
                        .extracting(Candidate::notionId)
                        .containsExactlyInAnyOrder(VARIABLES, TYPES));
    }

    @Test
    void shouldAskASingleQuestionOfADomainWithASingleNotion() {
        var domain = domain(candidates(VARIABLES, "variables", 4));

        assertThat(domain.questionCount()).isOne();
        assertThat(domain.draw(new Random(1))).hasSize(1);
    }

    @Test
    void shouldAskTheBeginnerNotionsOfEachDomainInTheOrderOfTheReferential() {
        NotionInfo variables = notion(VARIABLES, "bases", Level.BEGINNER);
        NotionInfo closures = notion(UUID.randomUUID(), "bases", Level.INTERMEDIATE);
        NotionInfo withoutQuestion = notion(TYPES, "outils", Level.BEGINNER);
        NotionInfo loops = notion(LOOPS, "outils", Level.BEGINNER);
        NotionInfo expert = notion(UUID.randomUUID(), "runtime", Level.EXPERT);
        NotionInfo unwritten = notion(UUID.randomUUID(), "vide", Level.BEGINNER);
        Map<UUID, List<UUID>> cards = Map.of(
                VARIABLES,
                List.of(UUID.randomUUID()),
                closures.id(),
                List.of(UUID.randomUUID()),
                LOOPS,
                List.of(UUID.randomUUID(), UUID.randomUUID()),
                expert.id(),
                List.of(UUID.randomUUID()));

        List<LevelCheckDomain> domains =
                LevelCheckDomain.of(List.of(variables, closures, withoutQuestion, loops, expert, unwritten), cards);

        assertThat(domains).extracting(LevelCheckDomain::id).containsExactly("bases", "outils");
        assertThat(domains).extracting(LevelCheckDomain::title).containsExactly("Domaine bases", "Domaine outils");
        assertThat(domains.get(0).beginnerNotions()).containsExactly(VARIABLES);
        // Une notion débutante sans question devient connue avec son domaine.
        assertThat(domains.get(1).beginnerNotions()).containsExactlyInAnyOrder(TYPES, LOOPS);
        assertThat(domains.get(1).candidates()).extracting(Candidate::notionId).containsOnly(LOOPS);
        assertThat(domains).extracting(LevelCheckDomain::questionCount).containsExactly(1, 1);
    }

    @Test
    void shouldAcceptTheCardsOfADrawOnly() {
        List<Candidate> variables = candidates(VARIABLES, "variables", 2);
        List<Candidate> types = candidates(TYPES, "types", 1);
        var domain = domain(variables, types);
        UUID elsewhere = UUID.randomUUID();

        assertThatCode(() -> domain.requireDrawnCards(domain.draw(new Random(7))))
                .doesNotThrowAnyException();
        assertThatCode(() -> domain.requireDrawnCards(
                        List.of(types.getFirst().cardId(), variables.getLast().cardId())))
                .doesNotThrowAnyException();
        // Deux cartes d'une même notion, une carte d'ailleurs, une réponse qui manque ou qui est en trop.
        assertInvalid(
                domain, List.of(variables.get(0).cardId(), variables.get(1).cardId()));
        assertInvalid(domain, List.of(variables.get(0).cardId(), elsewhere));
        assertInvalid(domain, List.of(variables.get(0).cardId()));
        assertInvalid(
                domain,
                List.of(
                        variables.get(0).cardId(),
                        types.getFirst().cardId(),
                        variables.get(1).cardId()));
    }

    @Test
    void shouldBeKnownOnceAllItsBeginnerNotionsAreKnown() {
        var domain = domain(candidates(VARIABLES, "variables", 1), candidates(TYPES, "types", 1));

        assertThat(domain.isKnown(Set.of(VARIABLES))).isFalse();
        assertThat(domain.isKnown(Set.of(VARIABLES, TYPES, LOOPS))).isTrue();
    }

    private static void assertInvalid(LevelCheckDomain domain, List<UUID> cardIds) {
        assertThatThrownBy(() -> domain.requireDrawnCards(cardIds))
                .isInstanceOf(InvalidLevelCheckAnswersException.class)
                .hasMessageContaining("Les bases");
    }

    private static List<Candidate> drawn(LevelCheckDomain domain, int seed) {
        Map<UUID, Candidate> byCard =
                domain.candidates().stream().collect(Collectors.toMap(Candidate::cardId, Function.identity()));
        return domain.draw(new Random(seed)).stream().map(byCard::get).toList();
    }

    @SafeVarargs
    private static LevelCheckDomain domain(List<Candidate>... notions) {
        List<Candidate> candidates = Stream.of(notions).flatMap(List::stream).toList();
        Set<UUID> beginners = candidates.stream().map(Candidate::notionId).collect(Collectors.toSet());
        return new LevelCheckDomain("bases", "Les bases", beginners, candidates);
    }

    /** Les questions possibles d'une notion : autant de cartes que demandé. */
    private static List<Candidate> candidates(UUID notion, String chapter, int cards) {
        return IntStream.range(0, cards)
                .mapToObj(_ -> new Candidate(UUID.randomUUID(), notion, chapter))
                .toList();
    }

    private static NotionInfo notion(UUID id, String domain, Level level) {
        return new NotionInfo(
                id,
                "go",
                "notion",
                "Notion",
                domain,
                "Domaine " + domain,
                domain + "-chapitre",
                "Chapitre",
                level,
                1,
                List.of(),
                0,
                1,
                0);
    }
}
