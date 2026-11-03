import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CalendrierTest {

    // le vendredi 14 mars 2025, à midi UTC
    private static final Clock VENDREDI = Clock.fixed(Instant.parse("2025-03-14T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void lAgeSeCompteEnAnneesRevolues() {
        Calendrier calendrier = new Calendrier(VENDREDI);

        assertEquals(35, calendrier.age(LocalDate.of(1990, 3, 14)));
        assertEquals(34, calendrier.age(LocalDate.of(1990, 3, 15)));
    }

    @Test
    void lesJoursAvantUneDatePeuventEtreNegatifs() {
        Calendrier calendrier = new Calendrier(VENDREDI);

        assertEquals(17, calendrier.joursAvant(LocalDate.of(2025, 3, 31)));
        assertEquals(-13, calendrier.joursAvant(LocalDate.of(2025, 3, 1)));
        assertEquals(0, calendrier.joursAvant(LocalDate.of(2025, 3, 14)));
    }

    @Test
    void unVendrediNEstPasUnWeekEnd() {
        assertFalse(new Calendrier(VENDREDI).estWeekEnd());
    }

    @Test
    void unSamediEstUnWeekEnd() {
        Clock samedi = Clock.fixed(Instant.parse("2025-03-15T12:00:00Z"), ZoneOffset.UTC);

        assertTrue(new Calendrier(samedi).estWeekEnd());
    }

    @Test
    void leJourDependDuFuseauDeLHorloge() {
        // 23 h 30 UTC le vendredi : il est déjà samedi à Paris
        Clock paris = Clock.fixed(Instant.parse("2025-03-14T23:30:00Z"), ZoneId.of("Europe/Paris"));

        assertTrue(new Calendrier(paris).estWeekEnd());
    }
}
