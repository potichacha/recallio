package bibliotheque;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LivreResourceTest {

    String json(String titre, String auteur, int annee) {
        return "{\"titre\": \"" + titre + "\", \"auteur\": \"" + auteur + "\", \"annee\": " + annee + "}";
    }

    Integer creer(String titre, String auteur, int annee) {
        return given().contentType(ContentType.JSON)
                .body(json(titre, auteur, annee))
                .when()
                .post("/livres")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    void creeUnLivreEtRepond201AvecSonAdresse() {
        ExtractableResponse<Response> reponse = given().contentType(ContentType.JSON)
                .body(json("Germinal", "Zola", 1885))
                .when()
                .post("/livres")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("titre", is("Germinal"))
                .body("auteur", is("Zola"))
                .extract();

        Integer id = reponse.path("id");
        String adresse = reponse.header("Location");
        assertNotNull(adresse, "La réponse doit avoir un en-tête Location");
        assertTrue(adresse.endsWith("/livres/" + id), "Location doit finir par /livres/" + id + " : " + adresse);
    }

    @Test
    void relitUnLivreParSonIdentifiant() {
        Integer id = creer("Candide", "Voltaire", 1759);

        given().when()
                .get("/livres/{id}", id)
                .then()
                .statusCode(200)
                .body("titre", is("Candide"))
                .body("auteur", is("Voltaire"))
                .body("annee", is(1759));
    }

    @Test
    void repond404PourUnLivreInconnu() {
        given().when().get("/livres/{id}", 987654).then().statusCode(404);
    }

    @Test
    void refuseUnLivreSansTitre() {
        given().contentType(ContentType.JSON)
                .body(json("", "Anonyme", 1900))
                .when()
                .post("/livres")
                .then()
                .statusCode(400)
                .body("violations.field", hasItem(containsString("titre")));
    }

    @Test
    void refuseUneAnneeAvantLImprimerie() {
        given().contentType(ContentType.JSON)
                .body(json("Manuscrit", "Moine", 1200))
                .when()
                .post("/livres")
                .then()
                .statusCode(400)
                .body("violations.field", hasItem(containsString("annee")));
    }

    @Test
    void listeLesLivresCrees() {
        creer("Bel-Ami", "Maupassant", 1885);
        creer("Le Horla", "Maupassant", 1887);

        given().when()
                .get("/livres")
                .then()
                .statusCode(200)
                .body("titre", hasItems("Bel-Ami", "Le Horla"));
    }
}
