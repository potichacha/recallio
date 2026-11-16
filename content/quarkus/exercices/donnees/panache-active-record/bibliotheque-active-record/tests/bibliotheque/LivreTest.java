package bibliotheque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestTransaction
class LivreTest {

    static void ranger(String titre, String auteur, boolean disponible) {
        Livre livre = new Livre();
        livre.titre = titre;
        livre.auteur = auteur;
        livre.disponible = disponible;
        livre.persist();
    }

    @Test
    void parAuteurRenvoieSesLivresTriesParTitre() {
        ranger("Les Misérables", "Victor Hugo", true);
        ranger("Germinal", "Émile Zola", true);
        ranger("Hernani", "Victor Hugo", false);

        List<String> titres = Livre.parAuteur("Victor Hugo").stream().map(livre -> livre.titre).toList();

        assertEquals(List.of("Hernani", "Les Misérables"), titres);
    }

    @Test
    void parTitreTrouveLeLivre() {
        ranger("Nana", "Émile Zola", true);

        Optional<Livre> livre = Livre.parTitre("Nana");

        assertTrue(livre.isPresent());
        assertEquals("Émile Zola", livre.get().auteur);
    }

    @Test
    void parTitreRenvoieVideQuandAucunLivreNePorteCeTitre() {
        assertTrue(Livre.parTitre("Un titre qui n'existe pas").isEmpty());
    }

    @Test
    void nombreDisponiblesNeCompteQueLesLivresDisponibles() {
        long avant = Livre.nombreDisponibles();
        ranger("Candide", "Voltaire", true);
        ranger("Zadig", "Voltaire", true);
        ranger("Micromégas", "Voltaire", false);

        assertEquals(avant + 2, Livre.nombreDisponibles());
    }
}
