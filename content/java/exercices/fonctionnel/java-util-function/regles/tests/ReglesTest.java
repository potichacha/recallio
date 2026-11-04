import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class ReglesTest {

    private final Predicate<String> nonVide = s -> !s.isEmpty();
    private final Predicate<String> court = s -> s.length() <= 5;
    private final Predicate<String> commenceParA = s -> s.startsWith("a");

    @Test
    void toutesExigeChaqueRegle() {
        Predicate<String> p = Regles.toutes(List.of(nonVide, court, commenceParA));

        assertTrue(p.test("abc"));
        assertFalse(p.test("abcdef"));
        assertFalse(p.test("bc"));
    }

    @Test
    void toutesEstVraiSansRegle() {
        assertTrue(Regles.<String>toutes(List.of()).test("x"));
    }

    @Test
    void auMoinsUneSuffitDUneRegle() {
        Predicate<String> p = Regles.auMoinsUne(List.of(court, commenceParA));

        assertTrue(p.test("abcdefgh"));
        assertTrue(p.test("b"));
        assertFalse(p.test("bcdefgh"));
    }

    @Test
    void auMoinsUneEstFauxSansRegle() {
        assertFalse(Regles.<String>auMoinsUne(List.of()).test("x"));
    }

    @Test
    void enchainerAppliqueLesEtapesDansLOrdre() {
        UnaryOperator<String> f = Regles.enchainer(List.of(String::strip, s -> s.substring(0, 3), String::toUpperCase));

        assertEquals("ABC", f.apply("  abcdef "));
    }

    @Test
    void enchainerSansEtapeRenvoieLaChaine() {
        assertEquals("tel quel", Regles.enchainer(List.of()).apply("tel quel"));
    }
}
