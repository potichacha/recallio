import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnsembleCompteTest {

    @Test
    void ajouterTousCompteChaqueElementUneSeuleFois() {
        EnsembleCompte ensemble = new EnsembleCompte();

        ensemble.ajouterTous(List.of("a", "b", "c"));

        assertEquals(3, ensemble.nombreAjouts());
        assertEquals(3, ensemble.taille());
    }

    @Test
    void lesDoublonsSontComptesMaisPasGardes() {
        EnsembleCompte ensemble = new EnsembleCompte();

        assertTrue(ensemble.ajouter("a"));
        assertFalse(ensemble.ajouter("a"));

        assertEquals(2, ensemble.nombreAjouts());
        assertEquals(1, ensemble.taille());
        assertTrue(ensemble.contient("a"));
    }

    @Test
    void lesDeuxFaconsDAjouterSeCumulent() {
        EnsembleCompte ensemble = new EnsembleCompte();

        ensemble.ajouter("x");
        ensemble.ajouterTous(List.of("y", "x"));

        assertEquals(3, ensemble.nombreAjouts());
        assertEquals(2, ensemble.taille());
        assertFalse(ensemble.contient("z"));
    }
}
