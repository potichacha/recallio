import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SaisiesTest {

    @Test
    void unEntierValideEstLu() {
        assertEquals(42, Saisies.entierOuDefaut("42", -1));
        assertEquals(-7, Saisies.entierOuDefaut("-7", -1));
    }

    @Test
    void lesBlancsAutourSontPermis() {
        assertEquals(12, Saisies.entierOuDefaut("  12 ", -1));
    }

    @Test
    void unTexteInvalideDonneLaValeurParDefaut() {
        assertEquals(-1, Saisies.entierOuDefaut("dix", -1));
        assertEquals(-1, Saisies.entierOuDefaut("", -1));
        assertEquals(-1, Saisies.entierOuDefaut(null, -1));
        assertEquals(-1, Saisies.entierOuDefaut("3.5", -1));
    }

    @Test
    void laSommeIgnoreLesElementsInvalides() {
        assertEquals(22, Saisies.somme("12, 7 ,x, 3"));
    }

    @Test
    void laSommeDUneListeVideVautZero() {
        assertEquals(0, Saisies.somme(""));
    }
}
