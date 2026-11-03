import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RemiseTest {

    @Test
    void aucuneRemiseEnDessousDe100Euros() {
        assertEquals(0, Remise.calculer(9_999, false));
    }

    @Test
    void cinqPourCentAPartirDe100Euros() {
        assertEquals(500, Remise.calculer(10_000, false));
        assertEquals(1_000, Remise.calculer(20_000, false));
    }

    @Test
    void dixPourCentAPartirDe500Euros() {
        assertEquals(2_497, Remise.calculer(49_950, false));
        assertEquals(5_000, Remise.calculer(50_000, false));
    }

    @Test
    void unClientFideleGagneCinqPoints() {
        assertEquals(400, Remise.calculer(8_000, true));
        assertEquals(1_500, Remise.calculer(15_000, true));
    }

    @Test
    void laRemiseNeDepassePas100Euros() {
        assertEquals(10_000, Remise.calculer(200_000, false));
        assertEquals(10_000, Remise.calculer(90_000, true));
    }

    @Test
    void arronditAuCentimeInferieur() {
        assertEquals(500, Remise.calculer(10_019, false));
    }

    @Test
    void refuseUnMontantNegatif() {
        assertThrows(IllegalArgumentException.class, () -> Remise.calculer(-1, false));
    }
}
