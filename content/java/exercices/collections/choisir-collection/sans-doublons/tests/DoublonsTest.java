import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DoublonsTest {

    @Test
    void retireLesDoublonsEnGardantLOrdreDeLaPremiereApparition() {
        assertEquals(List.of("b", "a", "c"), Doublons.sansDoublons(List.of("b", "a", "b", "c", "a")));
    }

    @Test
    void laisseIntacteUneListeSansDoublon() {
        assertEquals(List.of("z", "y", "x"), Doublons.sansDoublons(List.of("z", "y", "x")));
    }

    @Test
    void renvoieUneListeVidePourUneListeVide() {
        assertEquals(List.of(), Doublons.sansDoublons(List.of()));
    }

    @Test
    void neModifiePasLaListeRecue() {
        List<String> mots = new ArrayList<>(List.of("a", "a"));

        Doublons.sansDoublons(mots);

        assertEquals(List.of("a", "a"), mots);
    }
}
