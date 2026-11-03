import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EquipeTest {

    @Test
    void modifierLaListeDeDepartNeChangePasLEquipe() {
        List<String> depart = new ArrayList<>(List.of("Ada"));
        Equipe equipe = new Equipe("Pionniers", depart);

        depart.add("Bob");

        assertEquals(List.of("Ada"), equipe.membres());
    }

    @Test
    void lesMembresRendusNeSontPasModifiables() {
        Equipe equipe = new Equipe("Pionniers", new ArrayList<>(List.of("Ada")));

        assertThrows(UnsupportedOperationException.class, () -> equipe.membres().add("Bob"));
        assertEquals(List.of("Ada"), equipe.membres());
    }

    @Test
    void avecRenvoieUneNouvelleEquipeEtLaisseLOriginaleIntacte() {
        Equipe originale = new Equipe("Pionniers", new ArrayList<>(List.of("Ada")));

        Equipe agrandie = originale.avec("Grace");

        assertNotSame(originale, agrandie);
        assertEquals(List.of("Ada"), originale.membres());
        assertEquals(List.of("Ada", "Grace"), agrandie.membres());
        assertEquals("Pionniers", agrandie.nom());
    }

    @Test
    void unNomVideOuNullEstRefuse() {
        assertThrows(IllegalArgumentException.class, () -> new Equipe(" ", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Equipe(null, List.of()));
    }
}
