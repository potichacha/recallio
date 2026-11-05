package com.recallio.ranked.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.ranked.domain.Duel.DuelAnswer;
import com.recallio.ranked.domain.DuelDraw.Candidate;
import com.recallio.ranked.domain.Rank.Tier;
import com.recallio.ranked.domain.glicko.Glicko2.Game;
import com.recallio.ranked.domain.glicko.Rating;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Les règles du classé (ADR 0046) : rangs, saisons, placement, score d'un duel et tirage de ses questions. */
class RankedRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    @Test
    void shouldMapARatingToItsTierAndDivision() {
        assertThat(Rank.of(900)).isEqualTo(new Rank(Tier.BRONZE, 4));
        assertThat(Rank.of(1299)).isEqualTo(new Rank(Tier.BRONZE, 1));
        assertThat(Rank.of(1300)).isEqualTo(new Rank(Tier.SILVER, 4));
        assertThat(Rank.of(1500)).isEqualTo(new Rank(Tier.GOLD, 4));
        assertThat(Rank.of(1660)).isEqualTo(new Rank(Tier.GOLD, 1));
        assertThat(Rank.of(2099)).isEqualTo(new Rank(Tier.DIAMOND, 1));
        assertThat(Rank.of(2400)).isEqualTo(new Rank(Tier.MASTER, 0));
        assertThat(Rank.of(1510).isAbove(Rank.of(1499))).isTrue();
        assertThat(Rank.of(1560).isAbove(Rank.of(1510))).isTrue();
        assertThat(Rank.of(1510).isAbove(Rank.of(1520))).isFalse();
    }

    @Test
    void shouldCutTheYearInQuartersAndHalveTheDistanceTo1500AtANewSeason() {
        Season season = Season.of(NOW);

        assertThat(season.id()).isEqualTo("2026-T4");
        assertThat(Season.parse("2026-T4")).isEqualTo(season);
        assertThat(season.endsOn()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(Season.of(Instant.parse("2026-03-31T23:59:59Z")).id()).isEqualTo("2026-T1");

        Rating reset = Season.softReset(new Rating(1900, 60, 0.06));
        assertThat(reset.rating()).isEqualTo(1700);
        assertThat(reset.deviation()).isEqualTo(200);
    }

    @Test
    void shouldHideTheRankDuringThePlacementThenKeepThePeakOfTheSeason() {
        Season season = Season.of(NOW);
        Standing standing = Standing.start(UUID.randomUUID(), "go", season);
        for (int duel = 0; duel < Standing.PLACEMENT_DUELS - 1; duel++) {
            standing = standing.after(new Rating(1600, 200, 0.06), Game.WIN);
        }
        assertThat(standing.isPlacing()).isTrue();
        assertThat(standing.rank()).isEmpty();
        assertThat(standing.peakTier()).isEmpty();

        standing = standing.after(new Rating(1720, 150, 0.06), Game.WIN).after(new Rating(1650, 140, 0.06), Game.LOSS);

        assertThat(standing.rank()).contains(new Rank(Tier.GOLD, 1));
        assertThat(standing.peakTier()).contains(Tier.PLATINUM);
        assertThat(standing.wins()).isEqualTo(5);
        assertThat(standing.losses()).isEqualTo(1);

        Standing next = standing.inSeason(new Season(2027, 1));
        assertThat(next.isPlacing()).isTrue();
        assertThat(next.wins()).isZero();
        assertThat(next.rating().rating()).isEqualTo(1575);
    }

    @Test
    void shouldWinOnCorrectAnswersThenOnTime() {
        var fast = List.of(answer(0, 3, true), answer(1, 4, true));
        var slow = List.of(answer(0, 20, true), answer(1, 25, true));
        var wrong = List.of(answer(0, 1, false), answer(1, 1, true));

        assertThat(DuelScore.of(fast, 2).against(DuelScore.of(slow, 2))).isEqualTo(Game.WIN);
        assertThat(DuelScore.of(wrong, 2).against(DuelScore.of(slow, 2))).isEqualTo(Game.LOSS);
        assertThat(DuelScore.of(fast, 2).against(DuelScore.of(fast, 2))).isEqualTo(Game.DRAW);
        // Une question jamais servie compte tout son temps, comme une question sans réponse.
        assertThat(DuelScore.of(List.of(answer(0, 3, true)), 2).timeMs()).isEqualTo(33_000);
    }

    @Test
    void shouldDrawQuestionsCloseToTheLevelOneNotionAtATime() {
        UUID notion = UUID.randomUUID();
        List<Candidate> candidates = IntStream.range(0, 60)
                .mapToObj(index -> new Candidate(new UUID(0, index), index < 3 ? notion : null, 1000 + index * 20))
                .toList();

        List<UUID> drawn = DuelDraw.draw(candidates, 1500, 5, 42);

        assertThat(drawn).hasSize(5).doesNotHaveDuplicates();
        // Les 25 cartes les plus proches de 1500 vont de 1260 à 1740 : les indices 13 à 37.
        assertThat(drawn)
                .allMatch(card -> card.getLeastSignificantBits() >= 13 && card.getLeastSignificantBits() <= 37);
        assertThat(DuelDraw.draw(candidates, 1500, 5, 42)).isEqualTo(drawn);
    }

    private static DuelAnswer answer(int position, int seconds, boolean correct) {
        Instant served = NOW.plusSeconds(position * 60L);
        return new DuelAnswer(position, served, served.plusSeconds(seconds), correct);
    }
}
