package com.recallio.gamification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.CardType;
import com.recallio.gamification.domain.XpRules.ReviewedCard;
import com.recallio.gamification.domain.XpRules.XpAward;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class XpRulesTest {

    private final XpRules rules = new XpRules(10, 3, 50);

    @Test
    void shouldAwardTheSameXpWhateverTheRatingOfASelfAssessedCard() {
        var review = new XpAward(XpReason.REVIEW, 10, "java");

        assertThat(rules.awardsFor(card(CardType.FLASHCARD, true), 1)).containsExactly(review);
        assertThat(rules.awardsFor(card(CardType.FLASHCARD, false), 1)).containsExactly(review);
    }

    @ParameterizedTest
    @CsvSource({
        "FLASHCARD, 10",
        "DEFINITION, 10",
        "TRUE_FALSE, 10",
        "MULTIPLE_CHOICE, 10",
        "CODE_OUTPUT, 15",
        "FILL_IN, 15",
        "OPEN_SCENARIO, 20",
        "BUG_FIX, 20",
        "CODING_EXERCISE, 40"
    })
    void shouldScaleTheBaseXpWithTheEffortOfTheFormat(CardType type, int xp) {
        assertThat(rules.awardsFor(card(type, false), 1)).containsExactly(new XpAward(XpReason.REVIEW, xp, "java"));
    }

    @ParameterizedTest
    @CsvSource({"MULTIPLE_CHOICE, 10, 5", "TRUE_FALSE, 10, 5", "FILL_IN, 15, 8", "CODING_EXERCISE, 40, 20"})
    void shouldAddHalfTheBaseForASuccessGradedByTheServer(CardType type, int base, int bonus) {
        assertThat(rules.awardsFor(card(type, true), 1))
                .containsExactly(
                        new XpAward(XpReason.REVIEW, base, "java"),
                        new XpAward(XpReason.VERIFIED_SUCCESS, bonus, "java"));
    }

    @ParameterizedTest
    @EnumSource(
            value = CardType.class,
            names = {"FLASHCARD", "CODE_OUTPUT", "OPEN_SCENARIO", "DEFINITION", "BUG_FIX"})
    void shouldNotRewardASuccessDeclaredByTheUser(CardType type) {
        assertThat(rules.awardsFor(card(type, true), 1))
                .extracting(XpAward::reason)
                .containsExactly(XpReason.REVIEW);
    }

    @Test
    void shouldAwardTheDailyBonusToNoSubjectWhenTheGoalIsReached() {
        assertThat(rules.awardsFor(card(CardType.FLASHCARD, true), 3))
                .containsExactly(new XpAward(XpReason.REVIEW, 10, "java"), new XpAward(XpReason.DAILY_GOAL, 50, null));
    }

    @Test
    void shouldAwardTheDailyBonusOnlyOncePerDay() {
        assertThat(rules.awardsFor(card(CardType.FLASHCARD, true), 4))
                .containsExactly(new XpAward(XpReason.REVIEW, 10, "java"));
    }

    @Test
    void shouldAwardNothingWhenTheScaleIsZero() {
        assertThat(new XpRules(0, 3, 0).awardsFor(card(CardType.CODING_EXERCISE, true), 3))
                .isEmpty();
    }

    @Test
    void shouldRejectAGoalOfZeroCards() {
        assertThatThrownBy(() -> new XpRules(10, 0, 50)).isInstanceOf(IllegalArgumentException.class);
    }

    private static ReviewedCard card(CardType type, boolean recalled) {
        return new ReviewedCard("java", type, recalled);
    }
}
