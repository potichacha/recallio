package com.recallio.ranked.domain;

import com.recallio.ranked.domain.Duel.DuelAnswer;
import com.recallio.ranked.domain.glicko.Glicko2.Game;
import java.time.Duration;
import java.util.List;

/**
 * Le score d'un joueur dans un duel (ADR 0046) : ses bonnes réponses, puis son temps, mesuré par le serveur.
 *
 * @param timeMs le temps total compté ; une question sans réponse, ou jamais servie, compte tout son temps
 */
public record DuelScore(int correct, long timeMs) {

    /** @param questions les questions du duel, servies ou non */
    public static DuelScore of(List<DuelAnswer> answers, int questions) {
        int correct = (int) answers.stream().filter(DuelAnswer::correct).count();
        Duration time = answers.stream().map(DuelAnswer::time).reduce(Duration.ZERO, Duration::plus);
        Duration unserved = Duel.TIME_PER_QUESTION.multipliedBy(Math.max(0, questions - answers.size()));
        return new DuelScore(correct, time.plus(unserved).toMillis());
    }

    /** Le résultat de ce score contre l'autre : le plus de bonnes réponses gagne, puis le temps le plus court. */
    public double against(DuelScore other) {
        if (correct != other.correct) {
            return correct > other.correct ? Game.WIN : Game.LOSS;
        }
        if (timeMs != other.timeMs) {
            return timeMs < other.timeMs ? Game.WIN : Game.LOSS;
        }
        return Game.DRAW;
    }
}
