import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CaisseTest {

    @Test
    void additionneLesMontantsDesCommandesPayees() {
        List<Commande> commandes = List.of(new Commande("Ada", 1_250, true), new Commande("Alan", 800, true));

        assertEquals(2_050, Caisse.totalPaye(commandes));
    }

    @Test
    void ignoreLesCommandesNonPayees() {
        List<Commande> commandes = List.of(new Commande("Ada", 1_250, true), new Commande("Grace", 9_999, false));

        assertEquals(1_250, Caisse.totalPaye(commandes));
    }

    @Test
    void renvoieZeroSansCommande() {
        assertEquals(0, Caisse.totalPaye(List.of()));
    }
}
