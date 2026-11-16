package boutique;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ProduitResourceTest {

    @Test
    void creeUnProduit() {
        given().contentType(ContentType.JSON)
                .body("{\"nom\": \"stylo\", \"prix\": 2}")
                .when()
                .post("/produits")
                .then()
                .statusCode(201)
                .body("nom", is("stylo"));
    }

    @Test
    void refuseUnNomVide() {
        given().contentType(ContentType.JSON)
                .body("{\"nom\": \"\", \"prix\": 2}")
                .when()
                .post("/produits")
                .then()
                .statusCode(400);
    }

    @Test
    void listeLesProduits() {
        given().contentType(ContentType.JSON)
                .body("{\"nom\": \"cahier\", \"prix\": 5}")
                .post("/produits");

        given().when().get("/produits").then().statusCode(200).body("nom", hasItem("cahier"));
    }
}
