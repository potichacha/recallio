package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le carnet d'erreurs (ADR 0057) : les cartes ratées cette semaine et pas réussies depuis, à refaire sans laisser de
 * trace dans la progression. Chaque test a son propre utilisateur.
 */
@QuarkusTest
class MistakesTest {

    private static final String MISTAKES = "/api/v1/me/mistakes/erlang";

    private static final String SUBJECT = """
            subject: erlang
            title: Erlang
            category: languages
            position: 930
            status: published
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: premiers-pas
                title: Premiers pas
                notions:
                  - {id: atomes, title: "Les atomes", level: beginner, priority: 1}
                  - {id: tuples, title: "Les tuples", level: beginner, priority: 1}
                  - {id: listes, title: "Les listes", level: beginner, priority: 1}
                  - {id: gardes, title: "Les gardes", level: beginner, priority: 1}
            """;

    /** Une carte par notion, toutes vraies : « Vrai » est le choix 0, « Faux » le choix 1. */
    private static String notion(String id, String question) {
        return """
                notion: %s
                cards:
                  - {key: vrai-faux, format: true-false, question: "%s", truth: true, answer: "Oui."}
                """.formatted(id, question);
    }

    @Inject
    ContentImportService contentImport;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubject() {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT,
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of(
                        "bases/atomes.yaml", notion("atomes", "Un atome est une constante."),
                        "bases/tuples.yaml", notion("tuples", "Un tuple a une taille fixe."),
                        "bases/listes.yaml", notion("listes", "Une liste est chaînée."),
                        "bases/gardes.yaml", notion("gardes", "Une garde filtre une clause."))));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(MISTAKES).then().statusCode(401);
        check(UUID.randomUUID().toString(), 0).statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "mistakes-notebook"))
    void shouldListTheCardsFailedThisWeekAndNotRecalledSince() {
        follow();
        given().get(MISTAKES).then().statusCode(200).body("cards", empty());
        String atoms = card("atomes");
        String tuples = card("tuples");
        String lists = card("listes");
        String guards = card("gardes");

        answer(atoms, 1).body("rating", equalTo("AGAIN"));
        answer(tuples, 0).body("rating", equalTo("GOOD"));
        // Ratée, puis réussie : elle sort du carnet.
        answer(lists, 1).body("rating", equalTo("AGAIN"));
        answer(lists, 0).body("rating", equalTo("GOOD"));
        // Ratée il y a huit jours : la révision espacée l'a déjà reproposée.
        failEightDaysAgo(guards);

        given().get(MISTAKES)
                .then()
                .statusCode(200)
                .body("cards.cardId", contains(atoms))
                .body("cards[0].notionSlug", equalTo("atomes"))
                .body("cards[0].notionTitle", equalTo("Les atomes"))
                .body("cards[0].choices", contains("Vrai", "Faux"))
                .body("cards[0].answer", nullValue());
        // Le carnet d'un autre sujet, ou d'un sujet inconnu, est vide.
        given().get("/api/v1/me/mistakes/inconnu").then().statusCode(200).body("cards", empty());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "mistakes-check"))
    void shouldCorrectACardOfTheNotebookWithoutRecordingAnything() {
        follow();
        String atoms = card("atomes");
        String tuples = card("tuples");
        answer(atoms, 1);
        answer(tuples, 0);
        int cardsToday = progress().extract().path("cardsToday");
        int totalXp = progress().extract().path("totalXp");

        check(atoms, 0)
                .statusCode(200)
                .body("correct", is(true))
                .body("correctChoices", contains(0))
                .body("explanation", equalTo("Oui."));

        // Bien répondre dans le carnet n'est pas une révision : la carte y reste, et la progression ne bouge pas.
        given().get(MISTAKES).then().body("cards.cardId", contains(atoms));
        progress().body("cardsToday", is(cardsToday)).body("totalXp", is(totalXp));
        // Une carte qui n'est pas dans le carnet ne s'y corrige pas : la route ne donne pas les réponses à la demande.
        check(tuples, 0).statusCode(404).body("code", equalTo("review.card-not-found"));
        check(UUID.randomUUID().toString(), 0).statusCode(404);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "mistakes-lesson-link"))
    void shouldTellTheNotionOfEachCardOfTheReviewQueue() {
        follow();

        given().queryParam("subject", "erlang")
                .get("/api/v1/review-queue")
                .then()
                .statusCode(200)
                .body("cards.notionSlug", hasItem("atomes"));
    }

    private static void follow() {
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"erlang\"]}")
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }

    /** L'unique carte de la notion. */
    private static String card(String notion) {
        List<String> ids = given().get("/api/v1/me/practice/erlang/" + notion)
                .then()
                .statusCode(200)
                .extract()
                .path("cards.cardId");
        return ids.getFirst();
    }

    private static ValidatableResponse answer(String cardId, int choice) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("requestId", UUID.randomUUID(), "cardId", cardId, "selectedChoices", List.of(choice)))
                .post("/api/v1/reviews")
                .then()
                .statusCode(201);
    }

    private static ValidatableResponse check(String cardId, int choice) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("cardId", cardId, "selectedChoices", List.of(choice)))
                .post(MISTAKES + "/checks")
                .then();
    }

    private static ValidatableResponse progress() {
        return given().get("/api/v1/me/progress").then().statusCode(200);
    }

    private void failEightDaysAgo(String cardId) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                        insert into review_log (id, request_id, user_id, card_id, card_version_id, rating, reviewed_at,
                            state, stability, difficulty, due_at)
                        select gen_random_uuid(), gen_random_uuid(), u.id, :card, :card, 1, now() - interval '8 days',
                            'LEARNING', 1, 5, now()
                        from app_user u where u.oidc_subject = 'mistakes-notebook'
                        """)
                        .setParameter("card", UUID.fromString(cardId))
                        .executeUpdate());
    }
}
