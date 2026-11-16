package boutique;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CommandeResourceTest {

    @Inject
    Stock stock;

    ValidatableResponse commander(long produitId, int quantite) {
        return given().contentType(ContentType.JSON)
                .body("{\"produitId\": " + produitId + ", \"quantite\": " + quantite + "}")
                .when()
                .post("/commandes")
                .then();
    }

    @Test
    void passeUneCommandeQuandLeStockSuffit() {
        long cahier = stock.ajouter("cahier", 10);

        commander(cahier, 3).statusCode(201);
    }

    @Test
    void repond404AvecUnCodePourUnProduitInconnu() {
        commander(999999, 1)
                .statusCode(404)
                .body("code", is("PRODUIT_INTROUVABLE"))
                .body("message", containsString("999999"));
    }

    @Test
    void repond409AvecUnCodeQuandLeStockManque() {
        long stylo = stock.ajouter("stylo", 2);

        commander(stylo, 5)
                .statusCode(409)
                .body("code", is("STOCK_INSUFFISANT"))
                .body("message", containsString("stylo"));
    }

    @Test
    void laisseUnCheminInconnuEn404() {
        given().when().get("/chemin-inconnu").then().statusCode(404);
    }
}
