import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnsemblesTest {

    @Test
    void lesElementsCommuns() {
        assertEquals(Set.of(2, 3), Ensembles.communs(Set.of(1, 2, 3), Set.of(2, 3, 4)));
        assertEquals(Set.of(), Ensembles.communs(Set.of("a"), Set.of("b")));
    }

    @Test
    void laDifferenceSymetrique() {
        assertEquals(Set.of(1, 4), Ensembles.differenceSymetrique(Set.of(1, 2, 3), Set.of(2, 3, 4)));
    }

    @Test
    void lesArgumentsNeSontPasModifies() {
        Set<Integer> a = new HashSet<>(Set.of(1, 2, 3));
        Set<Integer> b = new HashSet<>(Set.of(2, 3, 4));

        Ensembles.communs(a, b);
        Ensembles.differenceSymetrique(a, b);

        assertEquals(Set.of(1, 2, 3), a);
        assertEquals(Set.of(2, 3, 4), b);
    }

    @Test
    void lesDoublonsDansLOrdreDeLeurPremiereRepetition() {
        Set<String> doublons = Ensembles.doublons(List.of("b", "a", "c", "a", "b", "a"));

        assertEquals(List.of("a", "b"), List.copyOf(doublons));
    }

    @Test
    void uneListeSansDoublonNEnDonneAucun() {
        assertEquals(Set.of(), Ensembles.doublons(List.of(1, 2, 3)));
    }
}
