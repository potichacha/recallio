package boutique;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PaiementTest {

    @InjectMock
    Banque banque;

    Integer creerCommande(String carte, int montant) {
        return given().contentType(ContentType.JSON)
                .body("{\"carte\": \"" + carte + "\", \"montant\": " + montant + "}")
                .when()
                .post("/commandes")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    @Test
    void payeLaCommandeEtGardeLaTransaction() {
        when(banque.debiter("4970-1111", 30)).thenReturn("TX-1");
        Integer id = creerCommande("4970-1111", 30);

        given().when()
                .post("/commandes/{id}/paiement", id)
                .then()
                .statusCode(200)
                .body("statut", is("PAYEE"))
                .body("transaction", is("TX-1"));

        verify(banque).debiter("4970-1111", 30);
    }

    @Test
    void repond402EtMarqueLaCommandeRefuseeQuandLaBanqueRefuse() {
        when(banque.debiter(anyString(), anyInt())).thenThrow(new PaiementRefuseException("Solde insuffisant"));
        Integer id = creerCommande("4970-2222", 500);

        given().when()
                .post("/commandes/{id}/paiement", id)
                .then()
                .statusCode(402)
                .body("statut", is("REFUSEE"));
    }

    @Test
    void neDebitePasDeuxFoisUneCommandePayee() {
        when(banque.debiter(anyString(), anyInt())).thenReturn("TX-2");
        Integer id = creerCommande("4970-3333", 12);

        given().when().post("/commandes/{id}/paiement", id).then().statusCode(200);
        given().when().post("/commandes/{id}/paiement", id).then().statusCode(409);

        verify(banque, times(1)).debiter("4970-3333", 12);
    }

    @Test
    void repond404PourUneCommandeInconnueSansAppelerLaBanque() {
        given().when().post("/commandes/{id}/paiement", 987654).then().statusCode(404);

        verifyNoInteractions(banque);
    }
}
