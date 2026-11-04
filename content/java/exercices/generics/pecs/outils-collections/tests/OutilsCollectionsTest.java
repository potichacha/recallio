import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class OutilsCollectionsTest {

    @Test
    void laSommeAccepteDesNombresDeTousTypes() {
        assertEquals(6.0, OutilsCollections.somme(List.of(1, 2, 3)));
        assertEquals(3.5, OutilsCollections.somme(List.of(1.5, 2.0)));
        assertEquals(0.0, OutilsCollections.somme(List.of()));
    }

    @Test
    void copierSiCopieDesEntiersDansUneListeDeNombres() {
        List<Integer> source = List.of(1, 2, 3, 4);
        List<Number> cible = new ArrayList<>();
        Predicate<Integer> pair = n -> n % 2 == 0;

        OutilsCollections.copierSi(source, pair, cible);

        assertEquals(List.of(2, 4), cible);
    }

    @Test
    void copierSiAccepteUnPredicatSurUnTypeParent() {
        List<String> source = new ArrayList<>(List.of("a", "b"));
        source.add(null);
        List<Object> cible = new ArrayList<>(List.of("déjà là"));
        Predicate<Object> nonNul = Objects::nonNull;

        OutilsCollections.copierSi(source, nonNul, cible);

        assertEquals(List.of("déjà là", "a", "b"), cible);
    }

    @Test
    void remplirAjouteLaValeurPlusieursFois() {
        List<Object> cible = new ArrayList<>();

        OutilsCollections.remplir(cible, "x", 3);

        assertEquals(List.of("x", "x", "x"), cible);
    }
}
