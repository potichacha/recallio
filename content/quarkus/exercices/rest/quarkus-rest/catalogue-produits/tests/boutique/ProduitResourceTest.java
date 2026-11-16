package boutique;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProduitResourceTest {

    ExtractableResponse<Response> creer(String nom, int prix) {
        return given().contentType(ContentType.JSON)
                .body("{\"nom\": \"" + nom + "\", \"prix\": " + prix + "}")
                .when()
                .post("/produits")
                .then()
                .statusCode(201)
                .extract();
    }

    @Test
    void creeUnProduitAvecLeStatut201EtSonAdresse() {
        ExtractableResponse<Response> reponse = creer("stylo", 2);

        int id = reponse.path("id");
        String nom = reponse.path("nom");
        assertEquals("stylo", nom);
        String location = reponse.header("Location");
        assertNotNull(location, "l'en-tête Location manque");
        assertTrue(location.endsWith("/produits/" + id), "Location vaut " + location);
    }

    @Test
    void retrouveUnProduitCree() {
        int id = creer("cahier", 5).path("id");

        given().when().get("/produits/" + id).then().statusCode(200).body("nom", is("cahier"));
    }

    @Test
    void repond404PourUnProduitInconnu() {
        given().when().get("/produits/999999").then().statusCode(404);
    }

    @Test
    void listeLesProduits() {
        creer("gomme", 1);

        given().when().get("/produits").then().statusCode(200).body("nom", hasItem("gomme"));
    }

    @Test
    void supprimeUnProduitAvecLeStatut204() {
        int id = creer("regle", 3).path("id");

        given().when().delete("/produits/" + id).then().statusCode(204);
        given().when().get("/produits/" + id).then().statusCode(404);
    }

    @Test
    void repond404QuandOnSupprimeUnProduitInconnu() {
        given().when().delete("/produits/999999").then().statusCode(404);
    }
}
