package bibliotheque;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LivreResourceTest {

    static int ajouter(String titre, String auteur) {
        return given().contentType(ContentType.JSON)
                .body("{\"titre\": \"" + titre + "\", \"auteur\": \"" + auteur + "\"}")
                .when()
                .post("/livres")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    void emprunterRendLeLivreIndisponible() {
        int id = ajouter("Le Horla", "Guy de Maupassant");

        given().when().post("/livres/" + id + "/emprunt").then().statusCode(204);

        given().when().get("/livres/" + id).then().statusCode(200).body("disponible", is(false));
    }

    @Test
    void emprunterUnLivreDejaEmprunteRepond409() {
        int id = ajouter("Bel-Ami", "Guy de Maupassant");

        given().when().post("/livres/" + id + "/emprunt").then().statusCode(204);

        given().when().post("/livres/" + id + "/emprunt").then().statusCode(409);
    }
}
