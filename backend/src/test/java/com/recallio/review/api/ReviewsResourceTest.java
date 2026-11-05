package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Chaque test se connecte avec un {@code sub} différent : chaque utilisateur a sa propre progression, ce qui isole les
 * tests les uns des autres. Le quota de nouvelles cartes vaut 3 en test (application.properties).
 */
@QuarkusTest
class ReviewsResourceTest {

    private static final String QUEUE = "/api/v1/review-queue";
    private static final String REVIEWS = "/api/v1/reviews";
    private static final String THEME = "java-fondamentaux";

    @Inject
    ContentImportService contentImport;

    /** Deux sujets publiés : Java (l'exemple de référence) et un petit document Go. */
    @BeforeEach
    void importSubjects() throws IOException {
        importDocument("/content/reference-sample.md");
        importDocument("/content/go-sample.md");
    }

    private void importDocument(String resource) throws IOException {
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(QUEUE).then().statusCode(401);
        given().contentType(ContentType.JSON).body("{}").post(REVIEWS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-new-user"))
    void shouldOfferNewCardsInProgramOrderWithinTheDailyLimit() {
        queue(THEME)
                .body("dueCount", is(0))
                .body("newCount", is(3))
                .body("cards", hasSize(3))
                .body("cards[0].prompt", containsString("JVM"))
                .body("cards[0].state", equalTo("NEW"))
                .body("cards[0].themeSlug", equalTo(THEME))
                .body("nextDueAt", nullValue());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-record"))
    void shouldScheduleAReviewedCardAndCountItAgainstTheDailyLimit() {
        String cardId = firstCardId();

        record(UUID.randomUUID(), cardId, "GOOD")
                .statusCode(201)
                .body("cardId", equalTo(cardId))
                .body("state", equalTo("LEARNING"))
                .body("dueAt", notNullValue());

        queue(THEME)
                .body("cards.cardId", not(contains(cardId)))
                .body("newCount", is(2))
                .body("nextDueAt", notNullValue());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-replay"))
    void shouldReturnTheSameResultWhenTheSameRequestIsSentTwice() {
        String cardId = firstCardId();
        UUID requestId = UUID.randomUUID();
        String dueAt =
                record(requestId, cardId, "EASY").statusCode(201).extract().path("dueAt");

        record(requestId, cardId, "EASY")
                .statusCode(200)
                .body("dueAt", equalTo(dueAt))
                .body("state", equalTo("REVIEW"));
        queue(THEME).body("newCount", is(2));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-conflict"))
    void shouldRejectARequestIdReusedForAnotherRating() {
        String cardId = firstCardId();
        UUID requestId = UUID.randomUUID();
        record(requestId, cardId, "GOOD").statusCode(201);

        record(requestId, cardId, "AGAIN").statusCode(409).body("code", equalTo("review.request-id-conflict"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-unknown-card"))
    void shouldRejectAnUnknownCard() {
        record(UUID.randomUUID(), UUID.randomUUID().toString(), "GOOD")
                .statusCode(404)
                .body("code", equalTo("review.card-not-found"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-invalid"))
    void shouldRejectAMissingRatingOrChoicesSentForASelfAssessedCard() {
        String cardId = firstCardId();
        given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\"}".formatted(UUID.randomUUID(), cardId))
                .post(REVIEWS)
                .then()
                .statusCode(422)
                .body("code", equalTo("review.answer-mismatch"));
        answer(UUID.randomUUID(), cardId, "[0]").statusCode(422).body("code", equalTo("review.answer-mismatch"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-multiple-choice-wrong"))
    void shouldGradeAWrongMultipleChoiceAnswerOnTheServer() {
        String cardId = multipleChoiceCard()
                .body("cards[0].answer", nullValue())
                .body("cards[0].choices", hasSize(3))
                .body("cards[0].multipleAnswers", is(false))
                .extract()
                .path("cards[0].cardId");

        record(UUID.randomUUID(), cardId, "EASY").statusCode(422).body("code", equalTo("review.answer-mismatch"));
        UUID requestId = UUID.randomUUID();
        answer(requestId, cardId, "[0]")
                .statusCode(201)
                .body("rating", equalTo("AGAIN"))
                .body("correction.correctChoices", contains(1))
                .body("correction.explanation", containsString("valeur zéro"));
        answer(requestId, cardId, "[0]").statusCode(200).body("correction.correctChoices", contains(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-multiple-choice-right"))
    void shouldGradeAnExactSelectionAsGood() {
        String cardId = multipleChoiceCard().extract().path("cards[0].cardId");

        answer(UUID.randomUUID(), cardId, "[1]").statusCode(201).body("rating", equalTo("GOOD"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-true-false"))
    void shouldGradeATrueOrFalseCardOnTheServerLikeATwoChoiceQuestion() {
        importScala();
        String cardId = given().queryParam("subject", "scala")
                .queryParam("type", "TRUE_FALSE")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards[0].choices", contains("Vrai", "Faux"))
                .body("cards[0].answer", nullValue())
                .extract()
                .path("cards[0].cardId");

        answer(UUID.randomUUID(), cardId, "[1]")
                .statusCode(201)
                .body("rating", equalTo("GOOD"))
                .body("correction.correctChoices", contains(1))
                .body("correction.explanation", equalTo("Non."));
    }

    /** Les deux cartes de la notion « val » (ADR 0031) : une par jour, la seconde attend le lendemain. */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-siblings"))
    void shouldOfferASingleCardOfANotionPerDay() {
        importScala();
        String cardId = given().queryParam("subject", "scala")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards", hasSize(1))
                .extract()
                .path("cards[0].cardId");

        answer(UUID.randomUUID(), cardId, "[1]").statusCode(201).body("rating", equalTo("GOOD"));

        given().queryParam("subject", "scala").get(QUEUE).then().statusCode(200).body("cards", hasSize(0));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-fill-in"))
    void shouldGradeEachBlankOfAFillInCardOnTheServer() {
        importScala();
        String cardId = given().queryParam("subject", "scala")
                .queryParam("type", "FILL_IN")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards[0].blankCount", is(2))
                .body("cards[0].answer", nullValue())
                .extract()
                .path("cards[0].cardId");

        record(UUID.randomUUID(), cardId, "GOOD").statusCode(422).body("code", equalTo("review.answer-mismatch"));
        fill(UUID.randomUUID(), cardId, "[\"val\"]").statusCode(422);
        UUID requestId = UUID.randomUUID();
        fill(requestId, cardId, "[\" val \", \"var\"]")
                .statusCode(201)
                .body("rating", equalTo("AGAIN"))
                .body("correction.blanks.expected", contains("val", "Int"))
                .body("correction.blanks.correct", contains(true, false))
                .body("correction.explanation", equalTo("val, puis le type."));
        fill(requestId, cardId, "[\" val \", \"var\"]")
                .statusCode(200)
                .body("correction.blanks.correct", contains(true, false));
        fill(UUID.randomUUID(), cardId, "[\"val\", \"scala.Int\"]")
                .statusCode(201)
                .body("rating", equalTo("GOOD"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-filters"))
    void shouldFilterByThemeAndType() {
        given().queryParam("theme", "inconnu").get(QUEUE).then().statusCode(404);

        given().queryParam("theme", THEME)
                .queryParam("type", "OPEN_SCENARIO")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards", hasSize(2))
                .body("cards.type", everyItem(equalTo("OPEN_SCENARIO")));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-no-subject"))
    void shouldOfferNothingWithoutFilterUntilTheUserFollowsASubject() {
        given().get(QUEUE).then().statusCode(200).body("cards", hasSize(0)).body("newCount", is(0));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-followed"))
    void shouldOfferOnlyTheFollowedSubjectsWithoutFilter() {
        follow("go");

        given().get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards", hasSize(3))
                .body("cards.subjectSlug", everyItem(equalTo("go")));
    }

    /** Chaque sujet a son quota de nouvelles cartes (ADR 0048), 3 en test ; une file qui en réunit plusieurs alterne. */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-two-subjects"))
    void shouldGiveEachFollowedSubjectItsOwnNewCardQuota() {
        follow("java", "go");

        given().get(QUEUE)
                .then()
                .statusCode(200)
                .body("newCount", is(6))
                .body("cards.subjectSlug", contains("java", "go", "java", "go", "java", "go"));
    }

    /** Le mode « questions corrigées » de la PWA (ADR 0048) : le document Go de test n'en a qu'une, son QCM. */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-graded-only"))
    void shouldReviewOnlyTheRequestedFormats() {
        given().queryParam("subject", "go")
                .queryParam("type", "MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_IN", "CODING_EXERCISE")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards.type", contains("MULTIPLE_CHOICE"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "review-one-subject"))
    void shouldReviewARequestedSubjectEvenIfNotFollowed() {
        follow("java");

        given().queryParam("subject", "go")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards.subjectSlug", contains("go", "go", "go"));
        given().queryParam("subject", "inconnu")
                .get(QUEUE)
                .then()
                .statusCode(404)
                .body("code", equalTo("content.subject-not-found"));
    }

    private static void follow(String... subjects) {
        String list = String.join("\", \"", subjects);
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"%s\"]}".formatted(list))
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }

    private static ValidatableResponse queue(String theme) {
        return given().queryParam("theme", theme).get(QUEUE).then().statusCode(200);
    }

    private static String firstCardId() {
        return queue(THEME).extract().path("cards[0].cardId");
    }

    /** Le QCM du document Go de test, seul de son format. */
    private static ValidatableResponse multipleChoiceCard() {
        return given().queryParam("subject", "go")
                .queryParam("type", "MULTIPLE_CHOICE")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards", hasSize(1));
    }

    /** Un sujet en dossier (ADR 0026) avec un vrai ou faux et un code à trous (ADR 0027). */
    private void importScala() {
        contentImport.importFolder(new SubjectFolder("""
                subject: scala
                title: Scala
                category: languages
                position: 920
                status: published
                """, Map.of("01-bases.yaml", """
                        domain: bases
                        title: Les bases
                        position: 10
                        chapters:
                          - id: valeurs
                            title: Valeurs
                            notions:
                              - {id: val, title: "val", level: beginner, priority: 1}
                        """), Map.of("bases/val.yaml", """
                        notion: val
                        cards:
                          - {key: reaffecter, format: true-false, question: "Un val se réaffecte.", truth: false, answer: "Non."}
                          - key: declarer
                            format: fill-in
                            question: "`___ x: ___ = 1`"
                            blanks: [[val], [Int, scala.Int]]
                            answer: "val, puis le type."
                        """)));
    }

    private static ValidatableResponse fill(UUID requestId, String cardId, String filledBlanks) {
        return given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"filledBlanks\": %s}"
                        .formatted(requestId, cardId, filledBlanks))
                .post(REVIEWS)
                .then();
    }

    private static ValidatableResponse answer(UUID requestId, String cardId, String selectedChoices) {
        return given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"selectedChoices\": %s}"
                        .formatted(requestId, cardId, selectedChoices))
                .post(REVIEWS)
                .then();
    }

    private static ValidatableResponse record(UUID requestId, String cardId, String rating) {
        return given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"rating\": \"%s\", \"elapsedMs\": 4200}"
                        .formatted(requestId, cardId, rating))
                .post(REVIEWS)
                .then();
    }
}
