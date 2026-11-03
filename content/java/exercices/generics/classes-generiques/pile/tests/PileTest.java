import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PileTest {

    @Test
    void leDernierEmpileEstLePremierDepile() {
        Pile<String> pile = new Pile<>();
        pile.empiler("a");
        pile.empiler("b");
        pile.empiler("c");

        assertEquals("c", pile.depiler());
        assertEquals("b", pile.depiler());
        assertEquals(1, pile.taille());
    }

    @Test
    void leSommetNeRetirePasLElement() {
        Pile<Integer> pile = new Pile<>();
        pile.empiler(42);

        assertEquals(Optional.of(42), pile.sommet());
        assertEquals(1, pile.taille());
    }

    @Test
    void uneNouvellePileEstVide() {
        Pile<Double> pile = new Pile<>();

        assertTrue(pile.estVide());
        assertEquals(Optional.empty(), pile.sommet());
    }

    @Test
    void depilerUnePileVideEstRefuse() {
        Pile<String> pile = new Pile<>();

        assertThrows(IllegalStateException.class, pile::depiler);
    }

    @Test
    void unePileSeVideApresAvoirToutDepile() {
        Pile<String> pile = new Pile<>();
        pile.empiler("x");
        pile.depiler();

        assertTrue(pile.estVide());
        assertFalse(pile.taille() > 0);
    }
}
