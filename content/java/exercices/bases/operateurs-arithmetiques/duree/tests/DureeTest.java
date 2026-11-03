import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DureeTest {

    @Test
    void decoupeEnHeuresMinutesEtSecondes() {
        assertEquals("1 h 2 min 5 s", Duree.enTexte(3725));
    }

    @Test
    void afficheZeroHeureEtZeroMinuteSousLaMinute() {
        assertEquals("0 h 0 min 59 s", Duree.enTexte(59));
    }

    @Test
    void afficheUneDureeNulle() {
        assertEquals("0 h 0 min 0 s", Duree.enTexte(0));
    }

    @Test
    void afficheLaDerniereSecondeDuneJournee() {
        assertEquals("23 h 59 min 59 s", Duree.enTexte(86_399));
    }
}
