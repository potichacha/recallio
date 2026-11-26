package com.recallio.review.domain.drill;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.NotionInfo.Level;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DrillLevelTest {

    private static final int RAREST_PRIORITY = 4;

    @Test
    void shouldAskAJuniorTheEssentialAndCommonNotionsOfBeginnersAndIntermediates() {
        assertThat(accepted(DrillLevel.JUNIOR, Level.BEGINNER)).containsExactly(1, 2);
        assertThat(accepted(DrillLevel.JUNIOR, Level.INTERMEDIATE)).containsExactly(1, 2);
        assertThat(accepted(DrillLevel.JUNIOR, Level.EXPERT)).isEmpty();
    }

    @Test
    void shouldAskAConfirmedDeveloperIntermediateAndExpertNotionsUpToTheAdvancedOnes() {
        assertThat(accepted(DrillLevel.CONFIRMED, Level.BEGINNER)).isEmpty();
        assertThat(accepted(DrillLevel.CONFIRMED, Level.INTERMEDIATE)).containsExactly(1, 2, 3);
        assertThat(accepted(DrillLevel.CONFIRMED, Level.EXPERT)).containsExactly(1, 2, 3);
    }

    @Test
    void shouldAskASeniorEveryIntermediateAndExpertNotion() {
        assertThat(accepted(DrillLevel.SENIOR, Level.BEGINNER)).isEmpty();
        assertThat(accepted(DrillLevel.SENIOR, Level.INTERMEDIATE)).containsExactly(1, 2, 3, 4);
        assertThat(accepted(DrillLevel.SENIOR, Level.EXPERT)).containsExactly(1, 2, 3, 4);
    }

    /** Les priorités des notions de ce niveau qui donnent des questions à ce poste. */
    private static List<Integer> accepted(DrillLevel drillLevel, Level notionLevel) {
        return IntStream.rangeClosed(1, RAREST_PRIORITY)
                .filter(priority -> drillLevel.accepts(notionLevel, priority))
                .boxed()
                .toList();
    }
}
