import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NombresTest {

    @Test
    void reconnaitDesNombresPremiers() {
        assertTrue(Nombres.estPremier(2));
        assertTrue(Nombres.estPremier(7));
        assertTrue(Nombres.estPremier(97));
    }

    @Test
    void ecarteLesNombresComposes() {
        assertFalse(Nombres.estPremier(9));
        assertFalse(Nombres.estPremier(100));
    }

    @Test
    void ecarteZeroUnEtLesNegatifs() {
        assertFalse(Nombres.estPremier(0));
        assertFalse(Nombres.estPremier(1));
        assertFalse(Nombres.estPremier(-5));
    }

    @Test
    void reconnaitUnCarreDePremier() {
        assertFalse(Nombres.estPremier(49));
    }
}
