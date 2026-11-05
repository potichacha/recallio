package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Valider une notion du Tour (ADR 0037) : ses questions, leur correction, et la progression qui en garde la trace. Chaque
 * test a son propre utilisateur.
 */
@QuarkusTest
class TourProgressTest {

    private static final String TOUR = "/api/v1/me/tour/ocaml";

    private static final String SUBJECT = """
            subject: ocaml
            title: OCaml
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
                  - {id: bonjour, title: "Le premier programme", level: beginner, priority: 1}
                  - {id: hors-tour, title: "Une notion sans page", level: beginner, priority: 1}
            """;

    /**
     * Deux pages (la seconde à la place du {@code %s}), puis un vrai ou faux (la réponse juste est « Faux »), un QCM (la
     * première) et un code à trous (« main »).
     */
    private static final String BONJOUR = """
            notion: bonjour
            tour:
              - text: Le premier programme.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            %s
            cards:
              - {key: cours, format: course, question: "Où commence un programme ?", answer: Dans main.}
              - {key: vrai-faux, format: true-false, question: "main prend des paramètres.", truth: false, answer: "Non."}
              - key: choix
                format: multiple-choice
                question: "Quel package rend un programme exécutable ?"
                choices:
                  - {text: "`main`", correct: true}
                  - {text: "`fmt`", correct: false}
                answer: "Le package `main`."
              - key: trou
                format: fill-in
                question: "`package ___`"
                blanks: [[main]]
                answer: "Le package main."
            """;

    /** La seconde page de « bonjour » : un test la retire, pour sortir cette page du Tour. */
    private static final String SECOND_PAGE = """
              - title: La suite
                text: Encore une page.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            """;

    /** Des cartes corrigées par le serveur, mais pas de page : la notion n'est pas dans le Tour. */
    private static final String HORS_TOUR = """
            notion: hors-tour
            cards:
              - {key: vrai-faux, format: true-false, question: "Cette notion a une page.", truth: false, answer: "Non."}
              - key: choix
                format: multiple-choice
                question: "Où vivent les pages du Tour ?"
                choices:
                  - {text: "Dans le bloc tour de la notion", correct: true}
                  - {text: "Dans un dossier à part", correct: false}
                answer: "Dans le bloc tour de la notion."
            """;

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importSubject() {
        importBonjour(SECOND_PAGE);
    }

    private void importBonjour(String secondPage) {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT,
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of("bases/bonjour.yaml", BONJOUR.formatted(secondPage), "bases/hors-tour.yaml", HORS_TOUR)));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(TOUR).then().statusCode(401);
        given().get(TOUR + "/bonjour").then().statusCode(401);
        openPage("bonjour", 1).statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-questions"))
    void shouldGiveTheQuestionsOfANotionWithoutTheirAnswers() {
        String body = given().get(TOUR + "/bonjour")
                .then()
                .statusCode(200)
                .body("validated", is(false))
                .body("questions.type", contains("TRUE_FALSE", "MULTIPLE_CHOICE", "FILL_IN"))
                .body("questions[0].choices", contains("Vrai", "Faux"))
                .body("questions[1].multipleAnswers", is(false))
                .body("questions[2].blankCount", is(1))
                .extract()
                .asString();

        assertThat(body).doesNotContain("correct", "Le package main.");
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-validation"))
    void shouldValidateTheNotionOnceEveryAnswerIsRightAndKeepItValidated() {
        List<String> ids = questionIds();

        check(answers(ids, 0, 0, "fmt"))
                .statusCode(200)
                .body("validated", is(false))
                .body("results.correct", contains(false, true, false))
                .body("results[0].correctChoices", contains(1))
                .body("results[2].blanks[0].expected", equalTo("main"))
                .body("results[0].explanation", equalTo("Non."));
        given().get(TOUR).then().statusCode(200).body("validatedNotions", empty());

        check(answers(ids, 1, 0, "main")).statusCode(200).body("validated", is(true));

        given().get(TOUR).then().body("validatedNotions", contains("bonjour"));
        given().get(TOUR + "/bonjour").then().body("validated", is(true));
        check(answers(ids, 0, 1, "fmt")).statusCode(200).body("validated", is(true));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-mismatch"))
    void shouldRefuseAnswersThatDoNotMatchTheQuestions() {
        List<String> ids = questionIds();

        check(List.of(choice(ids.get(0), 1))).statusCode(422).body("code", equalTo("review.answer-mismatch"));
        check(List.of(choice(ids.get(0), 1), choice(ids.get(1), 0), choice(ids.get(2), 0)))
                .statusCode(422)
                .body("code", equalTo("review.answer-mismatch"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-unknown"))
    void shouldNotFindAnUnknownNotionOrANotionOutsideTheTour() {
        given().get(TOUR + "/inconnue").then().statusCode(404).body("code", equalTo("review.tour-notion-not-found"));
        given().get(TOUR + "/hors-tour").then().statusCode(404).body("code", equalTo("review.tour-notion-not-found"));
        given().contentType(ContentType.JSON)
                .body(Map.of("answers", List.of(choice(UUID.randomUUID().toString(), 0))))
                .post(TOUR + "/hors-tour/checks")
                .then()
                .statusCode(404);
        given().get(TOUR).then().statusCode(200).body("validatedNotions", empty());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-last-page"))
    void shouldRememberTheLastPageOpenedToResumeThere() {
        given().get(TOUR).then().statusCode(200).body("lastPage", nullValue());

        openPage("bonjour", 2).statusCode(204);
        openPage("bonjour", 1).statusCode(204);

        given().get(TOUR).then().body("lastPage.notion", equalTo("bonjour")).body("lastPage.page", is(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-missing-page"))
    void shouldRefuseAPageThatIsNotInTheTour() {
        openPage("bonjour", 3).statusCode(404).body("code", equalTo("review.tour-page-not-found"));
        openPage("hors-tour", 1).statusCode(404).body("code", equalTo("review.tour-page-not-found"));
        openPage("inconnue", 1).statusCode(404).body("code", equalTo("review.tour-page-not-found"));
        openPage("bonjour", 0).statusCode(400);

        given().get(TOUR).then().body("lastPage", nullValue());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "tour-page-removed"))
    void shouldForgetALastPageThatLeftTheTour() {
        openPage("bonjour", 2).statusCode(204);

        importBonjour("");

        given().get(TOUR).then().statusCode(200).body("lastPage", nullValue());
    }

    private static ValidatableResponse openPage(String notion, int page) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("notion", notion, "page", page))
                .put(TOUR + "/last-page")
                .then();
    }

    private static List<String> questionIds() {
        return given().get(TOUR + "/bonjour").then().statusCode(200).extract().path("questions.cardId");
    }

    /** Une réponse à chaque question : le choix du vrai ou faux, celui du QCM, puis le trou. */
    private static List<Map<String, Object>> answers(List<String> ids, int truth, int choice, String blank) {
        return List.of(
                choice(ids.get(0), truth),
                choice(ids.get(1), choice),
                Map.of("cardId", ids.get(2), "filledBlanks", List.of(blank)));
    }

    private static Map<String, Object> choice(String cardId, int index) {
        return Map.of("cardId", cardId, "selectedChoices", List.of(index));
    }

    private static ValidatableResponse check(List<Map<String, Object>> answers) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("answers", answers))
                .post(TOUR + "/bonjour/checks")
                .then();
    }
}
