import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PanierTest {

    @Test
    void onRetrouveLesArticlesAjoutesDansLOrdre() {
        Panier panier = new Panier();
        panier.ajouter("pomme");
        panier.ajouter("poire");

        assertEquals(List.of("pomme", "poire"), panier.articles());
        assertEquals(2, panier.taille());
    }

    @Test
    void unArticleVideOuNullEstRefuse() {
        Panier panier = new Panier();

        assertThrows(IllegalArgumentException.class, () -> panier.ajouter(null));
        assertThrows(IllegalArgumentException.class, () -> panier.ajouter(" "));
        assertEquals(0, panier.taille());
    }

    @Test
    void unOnziemeArticleEstRefuse() {
        Panier panier = new Panier();
        for (int i = 1; i <= 10; i++) {
            panier.ajouter("article " + i);
        }

        assertThrows(IllegalStateException.class, () -> panier.ajouter("de trop"));
        assertEquals(10, panier.taille());
    }

    @Test
    void lesArticlesRendusNePermettentPasDeModifierLePanier() {
        Panier panier = new Panier();
        panier.ajouter("pomme");

        assertThrows(UnsupportedOperationException.class, () -> panier.articles().add("vélo"));
        assertEquals(1, panier.taille());
    }

    @Test
    void retirerDitSiLArticleEtaitPresent() {
        Panier panier = new Panier();
        panier.ajouter("pomme");

        assertTrue(panier.retirer("pomme"));
        assertFalse(panier.retirer("pomme"));
        assertEquals(0, panier.taille());
    }
}
