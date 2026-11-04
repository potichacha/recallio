import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextesTest {

    @Test
    void lesInitialesSontEnMajuscules() {
        assertEquals("AL", Textes.initiales("ada lovelace"));
        assertEquals("GMH", Textes.initiales("Grace Murray Hopper"));
    }

    @Test
    void lesInitialesToleerentPlusieursEspaces() {
        assertEquals("AT", Textes.initiales("  alan   turing "));
    }

    @Test
    void unPalindromeIgnoreLesEspacesEtLaCasse() {
        assertTrue(Textes.estPalindrome("Esope reste ici et se repose"));
        assertTrue(Textes.estPalindrome("kayak"));
        assertFalse(Textes.estPalindrome("java"));
    }

    @Test
    void masquerGardeLesQuatreDerniersCaracteres() {
        assertEquals("************9012", Textes.masquer("4970123456789012"));
        assertEquals("*2345", Textes.masquer("12345"));
    }

    @Test
    void unTexteCourtNEstPasMasque() {
        assertEquals("1234", Textes.masquer("1234"));
        assertEquals("", Textes.masquer(""));
    }
}
