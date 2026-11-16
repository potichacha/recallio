package boutique;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProduitResourceTest {

    ValidatableResponse poster(String json) {
        return given().contentType(ContentType.JSON).body(json).when().post("/produits").then();
    }

    @Test
    void accepteUnProduitValide() {
        poster("{\"nom\": \"stylo\", \"prix\": 2, \"reference\": \"STY-001\"}")
                .statusCode(201)
                .body("nom", is("stylo"));
    }

    @Test
    void refuseUnNomVide() {
        poster("{\"nom\": \"   \", \"prix\": 2, \"reference\": \"STY-001\"}")
                .statusCode(400)
                .body("violations.field", hasItem(endsWith("nom")));
    }

    @Test
    void refuseUnNomDePlusDe50Caracteres() {
        String nom = "x".repeat(51);
        poster("{\"nom\": \"" + nom + "\", \"prix\": 2, \"reference\": \"STY-001\"}").statusCode(400);
    }

    @Test
    void refuseUnPrixNul() {
        poster("{\"nom\": \"stylo\", \"prix\": 0, \"reference\": \"STY-001\"}")
                .statusCode(400)
                .body("violations.field", hasItem(endsWith("prix")));
    }

    @Test
    void refuseUneReferenceMalFormee() {
        poster("{\"nom\": \"stylo\", \"prix\": 2, \"reference\": \"sty-1\"}")
                .statusCode(400)
                .body("violations.field", hasItem(endsWith("reference")));
    }

    @Test
    void refuseUneReferenceAbsente() {
        poster("{\"nom\": \"stylo\", \"prix\": 2}").statusCode(400);
    }

    @Test
    void refuseUneRequeteSansCorps() {
        given().contentType(ContentType.JSON).when().post("/produits").then().statusCode(400);
    }

    @Test
    void borneLaTailleDeLaPage() {
        given().when().get("/produits?taille=0").then().statusCode(400);
        given().when().get("/produits?taille=101").then().statusCode(400);
        given().when().get("/produits?taille=10").then().statusCode(200);
        given().when().get("/produits").then().statusCode(200);
    }
}
