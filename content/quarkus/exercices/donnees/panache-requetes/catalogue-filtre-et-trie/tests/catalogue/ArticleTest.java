package catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestTransaction
class ArticleTest {

    static void ranger(String nom, String categorie, int prix, int stock) {
        Article article = new Article();
        article.nom = nom;
        article.categorie = categorie;
        article.prix = prix;
        article.stock = stock;
        article.persist();
    }

    static List<String> noms(List<Article> articles) {
        return articles.stream().map(article -> article.nom).toList();
    }

    @Test
    void deCategorieRangeDuMoinsCherAuPlusCher() {
        ranger("arrosoir", "jardin", 15, 3);
        ranger("tondeuse", "jardin", 240, 1);
        ranger("casserole", "cuisine", 30, 5);
        ranger("sécateur", "jardin", 9, 0);

        assertEquals(List.of("sécateur", "arrosoir", "tondeuse"), noms(Article.deCategorie("jardin")));
    }

    @Test
    void entreGardeLesBornesEtTrieParPrixPuisParNom() {
        ranger("louche", "ustensiles", 1001, 4);
        ranger("fouet", "ustensiles", 1005, 2);
        ranger("chinois", "ustensiles", 1005, 1);
        ranger("passoire", "ustensiles", 1010, 6);
        ranger("mixeur", "ustensiles", 1011, 2);

        assertEquals(List.of("louche", "chinois", "fouet", "passoire"), noms(Article.entre(1001, 1010)));
    }

    @Test
    void nombreEnRuptureNeCompteQueLesStocksAZero() {
        long avant = Article.nombreEnRupture();
        ranger("bêche", "jardin", 25, 0);
        ranger("râteau", "jardin", 18, 0);
        ranger("brouette", "jardin", 80, 2);

        assertEquals(avant + 2, Article.nombreEnRupture());
    }

    @Test
    void leMoinsCherTrouveLArticleAuPlusPetitPrix() {
        ranger("lampe", "salon", 45, 1);
        ranger("coussin", "salon", 12, 8);
        ranger("tapis", "salon", 90, 2);

        Optional<Article> article = Article.leMoinsCher("salon");

        assertTrue(article.isPresent());
        assertEquals("coussin", article.get().nom);
    }

    @Test
    void leMoinsCherRenvoieVidePourUneCategorieSansArticle() {
        assertTrue(Article.leMoinsCher("catégorie inconnue").isEmpty());
    }
}
