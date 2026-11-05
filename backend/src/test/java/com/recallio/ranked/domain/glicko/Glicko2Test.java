package com.recallio.ranked.domain.glicko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.recallio.ranked.domain.glicko.Glicko2.Game;
import java.util.List;
import org.junit.jupiter.api.Test;

class Glicko2Test {

    /** L'exemple de l'article de Glickman (« Example of the Glicko-2 system », 2012). */
    @Test
    void shouldReproduceTheExampleOfTheGlicko2Paper() {
        Rating player = new Rating(1500, 200, 0.06);
        List<Game> games = List.of(
                new Game(new Rating(1400, 30, 0.06), Game.WIN),
                new Game(new Rating(1550, 100, 0.06), Game.LOSS),
                new Game(new Rating(1700, 300, 0.06), Game.LOSS));

        Rating updated = Glicko2.update(player, games);

        assertThat(updated.rating()).isCloseTo(1464.06, within(0.01));
        assertThat(updated.deviation()).isCloseTo(151.52, within(0.01));
        assertThat(updated.volatility()).isCloseTo(0.05999, within(0.00001));
    }

    @Test
    void shouldOnlyGrowTheUncertaintyWithoutGamesAndNeverBeyondAStartingOne() {
        Rating player = new Rating(1700, 50, 0.06);

        Rating idle = Glicko2.update(player, List.of());

        assertThat(idle.rating()).isEqualTo(1700);
        assertThat(idle.deviation()).isGreaterThan(50);
        assertThat(Glicko2.update(Rating.START, List.of()).deviation()).isEqualTo(Rating.START_DEVIATION);
    }

    @Test
    void shouldMoveANewPlayerFarMoreThanASettledOne() {
        Rating opponent = new Rating(1500, 50, 0.06);
        Rating settled = new Rating(1500, 50, 0.06);

        double newcomerGain = Glicko2.update(Rating.START, List.of(new Game(opponent, Game.WIN)))
                        .rating()
                - 1500;
        double settledGain =
                Glicko2.update(settled, List.of(new Game(opponent, Game.WIN))).rating() - 1500;

        assertThat(newcomerGain).isGreaterThan(5 * settledGain).isPositive();
        assertThat(Glicko2.expectedScore(settled, opponent)).isCloseTo(0.5, within(0.0001));
    }
}
