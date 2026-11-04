import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParenthesesTest {

    @Test
    void accepteDesDelimiteursImbriques() {
        assertTrue(Parentheses.equilibree("(a[b]{c})"));
        assertTrue(Parentheses.equilibree("f(x) + [y] * {z}"));
    }

    @Test
    void accepteUnTexteVide() {
        assertTrue(Parentheses.equilibree(""));
    }

    @Test
    void refuseUnFermantQuiNeCorrespondPas() {
        assertFalse(Parentheses.equilibree("(]"));
        assertFalse(Parentheses.equilibree("([)]"));
    }

    @Test
    void refuseUnOuvrantJamaisFerme() {
        assertFalse(Parentheses.equilibree("(()"));
    }

    @Test
    void refuseUnFermantSansOuvrant() {
        assertFalse(Parentheses.equilibree(")("));
    }
}
