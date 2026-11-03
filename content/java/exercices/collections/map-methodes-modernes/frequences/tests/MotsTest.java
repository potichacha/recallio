import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MotsTest {

    @Test
    void compteLesOccurrencesSansTenirCompteDeLaCasse() {
        assertEquals(Map.of("java", 3, "go", 1), Mots.frequences(List.of("Java", "go", "java", "JAVA")));
        assertEquals(Map.of(), Mots.frequences(List.of()));
    }

    @Test
    void regroupeParInitialeDansLOrdreDesInitiales() {
        Map<Character, List<String>> groupes = Mots.parInitiale(List.of("bob", "Ada", "alan", "carl", "Bea"));

        assertEquals(Map.of('a', List.of("Ada", "alan"), 'b', List.of("bob", "Bea"), 'c', List.of("carl")), groupes);
        assertEquals(List.of('a', 'b', 'c'), List.copyOf(groupes.keySet()));
    }

    @Test
    void trouveLeMotLePlusFrequent() {
        assertEquals("java", Mots.plusFrequent(List.of("go", "Java", "java", "rust")));
    }

    @Test
    void departageLesEgalitesParOrdreAlphabetique() {
        assertEquals("go", Mots.plusFrequent(List.of("rust", "go", "rust", "go")));
    }

    @Test
    void renvoieUneChaineVidePourUneListeVide() {
        assertEquals("", Mots.plusFrequent(List.of()));
    }
}
