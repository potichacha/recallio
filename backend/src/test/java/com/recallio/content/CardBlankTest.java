package com.recallio.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recallio.content.domain.CardContent;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardBlankTest {

    private final CardBlank hash = new CardBlank(List.of("Objects.hash(x, y)", "Objects.hash(y, x)"));

    @Test
    void shouldAcceptAnyAcceptedAnswerWhateverTheSpaces() {
        assertThat(hash.accepts("Objects.hash(x,y)")).isTrue();
        assertThat(hash.accepts("  Objects.hash( y , x ) ")).isTrue();
        assertThat(hash.expected()).isEqualTo("Objects.hash(x, y)");
    }

    @Test
    void shouldRespectTheCaseLikeCode() {
        assertThat(hash.accepts("objects.hash(x, y)")).isFalse();
        assertThat(hash.accepts("")).isFalse();
    }

    @Test
    void shouldRefuseABlankWithoutAnswer() {
        assertThatThrownBy(() -> new CardBlank(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CardBlank(List.of(" "))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRequireOneAnswerListPerMarkerOfTheQuestion() {
        var one = List.of(new CardBlank(List.of("hashCode")));

        assertThat(new CardContent.FillIn("`public int ___()`", "R.", one).blanks())
                .hasSize(1);
        assertThatThrownBy(() -> new CardContent.FillIn("`___ ___`", "R.", one))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 trou(s)");
        assertThatThrownBy(() -> new CardContent.FillIn("Sans trou", "R.", one))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
