import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrisTest {

    private static final Employe BOB = new Employe("Bob", "IT", 3000);
    private static final Employe ADA = new Employe("ada", "RH", 3500);
    private static final Employe CY = new Employe("Cy", "IT", 4000);
    private static final Employe ALAN = new Employe("Alan", "RH", 2800);

    private static List<String> noms(List<Employe> employes) {
        return employes.stream().map(Employe::nom).toList();
    }

    @Test
    void parServicePuisSalaireDecroissant() {
        List<Employe> tries = Tris.parServicePuisSalaireDecroissant(List.of(BOB, ADA, CY, ALAN));

        assertEquals(List.of("Cy", "Bob", "ada", "Alan"), noms(tries));
    }

    @Test
    void parNomSansTenirCompteDeLaCasse() {
        List<Employe> tries = Tris.parNomSansCasse(List.of(BOB, ALAN, CY, ADA));

        assertEquals(List.of("ada", "Alan", "Bob", "Cy"), noms(tries));
    }

    @Test
    void lesMieuxPayesDuPlusAuMoinsPaye() {
        List<Employe> tries = Tris.mieuxPayes(List.of(BOB, ADA, CY, ALAN), 2);

        assertEquals(List.of("Cy", "ada"), noms(tries));
    }

    @Test
    void laListeDOrigineNEstPasModifiee() {
        List<Employe> origine = new ArrayList<>(List.of(BOB, ADA, CY));

        Tris.parServicePuisSalaireDecroissant(origine);
        Tris.mieuxPayes(origine, 1);

        assertEquals(List.of("Bob", "ada", "Cy"), noms(origine));
    }
}
