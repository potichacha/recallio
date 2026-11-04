import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AbonnementTest {

    private static final LocalDate FIN = LocalDate.of(2020, 3, 31);

    private static Clock le(int annee, int mois, int jour) {
        return Clock.fixed(LocalDate.of(annee, mois, jour).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @Test
    void estValideAvantLaDateDeFin() {
        assertFalse(new Abonnement(FIN, le(2020, 3, 1)).estExpire());
    }

    @Test
    void estEncoreValideLeDernierJour() {
        assertFalse(new Abonnement(FIN, le(2020, 3, 31)).estExpire());
    }

    @Test
    void estExpireLeLendemainDeLaFin() {
        assertTrue(new Abonnement(FIN, le(2020, 4, 1)).estExpire());
    }

    @Test
    void compteLesJoursRestants() {
        assertEquals(30, new Abonnement(FIN, le(2020, 3, 1)).joursRestants());
        assertEquals(0, new Abonnement(FIN, le(2020, 3, 31)).joursRestants());
    }

    @Test
    void neRenvoieJamaisUnNombreNegatif() {
        assertEquals(0, new Abonnement(FIN, le(2020, 5, 10)).joursRestants());
    }
}
