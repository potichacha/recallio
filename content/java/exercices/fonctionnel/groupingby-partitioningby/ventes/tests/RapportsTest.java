import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RapportsTest {

    private final List<Vente> ventes = List.of(
            new Vente("lea", "ouest", 100),
            new Vente("max", "est", 300),
            new Vente("ada", "ouest", 250),
            new Vente("lea", "ouest", 50),
            new Vente("bob", "est", 20));

    @Test
    void totalParRegionTrieParRegion() {
        Map<String, Integer> totaux = Rapports.totalParRegion(ventes);

        assertEquals(Map.of("est", 320, "ouest", 400), totaux);
        assertEquals(List.of("est", "ouest"), List.copyOf(totaux.keySet()));
    }

    @Test
    void vendeursParRegionTriesEtSansDoublon() {
        Map<String, Set<String>> vendeurs = Rapports.vendeursParRegion(ventes);

        assertEquals(List.of("est", "ouest"), List.copyOf(vendeurs.keySet()));
        assertEquals(List.of("bob", "max"), List.copyOf(vendeurs.get("est")));
        assertEquals(List.of("ada", "lea"), List.copyOf(vendeurs.get("ouest")));
    }

    @Test
    void grossesVentesSepareSelonLeSeuil() {
        Map<Boolean, List<Vente>> groupes = Rapports.grossesVentes(ventes, 250);

        assertEquals(List.of(ventes.get(1), ventes.get(2)), groupes.get(true));
        assertEquals(List.of(ventes.get(0), ventes.get(3), ventes.get(4)), groupes.get(false));
    }

    @Test
    void grossesVentesGardeLesDeuxClesMemeVides() {
        Map<Boolean, List<Vente>> groupes = Rapports.grossesVentes(ventes, 10_000);

        assertEquals(List.of(), groupes.get(true));
        assertEquals(5, groupes.get(false).size());
    }

    @Test
    void aucuneVenteDonneDesRapportsVides() {
        assertEquals(Map.of(), Rapports.totalParRegion(List.of()));
        assertEquals(Map.of(false, List.of(), true, List.of()), Rapports.grossesVentes(List.of(), 1));
    }
}
