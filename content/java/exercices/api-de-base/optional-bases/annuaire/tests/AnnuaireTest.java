import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnnuaireTest {

    private final Annuaire annuaire = new Annuaire(Map.of(
            "ada", "ada@exemple.fr",
            "alan", "alan@universite.uk"));

    @Test
    void unNomConnuDonneSonEmail() {
        assertEquals(Optional.of("ada@exemple.fr"), annuaire.email("ada"));
    }

    @Test
    void unNomInconnuDonneUnOptionalVide() {
        assertTrue(annuaire.email("grace").isEmpty());
    }

    @Test
    void leDomaineEstCeQuiSuitLArobase() {
        assertEquals("exemple.fr", annuaire.domaine("ada"));
        assertEquals("universite.uk", annuaire.domaine("alan"));
    }

    @Test
    void leDomaineDUnInconnuEstInconnu() {
        assertEquals("inconnu", annuaire.domaine("grace"));
    }

    @Test
    void lesEmailsConnusIgnorentLesInconnusEtGardentLOrdre() {
        assertEquals(List.of("alan@universite.uk", "ada@exemple.fr"),
                annuaire.emailsConnus(List.of("alan", "grace", "ada")));
    }
}
