package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.review.FakeCodeRunner;
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
 * Les exercices de code en révision (ADR 0030) : les essayer, puis les rendre, jugés par leurs tests. Le runner est
 * remplacé par {@link FakeCodeRunner} ; chaque test a son propre utilisateur.
 */
@QuarkusTest
class ExerciseReviewsTest {

    private static final String QUEUE = "/api/v1/review-queue";
    private static final String REVIEWS = "/api/v1/reviews";
    private static final String RUNS = "/api/v1/exercise-runs";
    private static final String STARTER = "class Doublons { /* départ */ }";
    private static final String MY_CODE = "class Doublons { /* ma version */ }";

    @Inject
    ContentImportService contentImport;

    @Inject
    FakeCodeRunner runner;

    @BeforeEach
    void importAnExercise() {
        runner.willReturn(FakeCodeRunner.PASSED);
        contentImport.importFolder(new SubjectFolder(
                """
                subject: elixir
                title: Elixir
                category: languages
                position: 930
                status: published
                """,
                Map.of("01-bases.yaml", """
                        domain: bases
                        title: Les bases
                        position: 10
                        chapters:
                          - id: listes
                            title: Listes
                            notions:
                              - {id: listes, title: "Les listes", level: beginner, priority: 1}
                        """),
                Map.of("bases/listes.yaml", """
                        notion: listes
                        cards:
                          - {key: tete, format: course, question: "La tête d'une liste ?", answer: "hd."}
                        """),
                Map.of(
                        "bases/listes/sans-doublons/exercise.yaml",
                        "statement: Retirer les doublons.\nexplanation: Avec un LinkedHashSet.\n",
                        "bases/listes/sans-doublons/starter/Doublons.java",
                        STARTER,
                        "bases/listes/sans-doublons/solution/Doublons.java",
                        "class Doublons { /* solution */ }",
                        "bases/listes/sans-doublons/tests/DoublonsTest.java",
                        "class DoublonsTest {}")));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exercise-queue"))
    void shouldOfferTheStarterCodeAndTheTestsButNotTheSolution() {
        exercise()
                .body("cards[0].exercise.starter.path", contains("Doublons.java"))
                .body("cards[0].exercise.tests.path", contains("DoublonsTest.java"))
                .body("cards[0].exercise", not(hasKey("solution")))
                .body("cards[0].answer", equalTo(null));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exercise-try"))
    void shouldRunTheCodeAgainstTheTestsOfTheExerciseWithoutRecordingAnything() {
        String cardId = exerciseId();
        runner.willReturn(FakeCodeRunner.FAILED);

        given().contentType(ContentType.JSON)
                .body(Map.of("cardId", cardId, "files", files(MY_CODE)))
                .post(RUNS)
                .then()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("tests.name", contains("retireLesDoublons"))
                .body("tests[0].message", equalTo("expected: <[b, a]> but was: <[b, a, b]>"));

        assertThat(runner.lastCode()).containsExactly(new SourceFile("Doublons.java", MY_CODE));
        assertThat(runner.lastTests()).containsExactly(new SourceFile("DoublonsTest.java", "class DoublonsTest {}"));
        exercise().body("cards[0].state", equalTo("NEW"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exercise-wrong-files"))
    void shouldRefuseFilesThatAreNotThoseOfTheStarterCode() {
        String cardId = exerciseId();

        given().contentType(ContentType.JSON)
                .body(Map.of("cardId", cardId, "files", List.of(Map.of("path", "Autre.java", "content", "x"))))
                .post(RUNS)
                .then()
                .statusCode(422)
                .body("code", equalTo("review.answer-mismatch"));
        given().contentType(ContentType.JSON)
                .body(Map.of("cardId", flashcardId(), "files", files(MY_CODE)))
                .post(RUNS)
                .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exercise-submit"))
    void shouldGradeASubmittedExerciseByItsTestsAndShowTheSolution() {
        String cardId = exerciseId();
        UUID requestId = UUID.randomUUID();

        submit(requestId, cardId)
                .statusCode(201)
                .body("rating", equalTo("GOOD"))
                .body("correction.exerciseRun.status", equalTo("PASSED"))
                .body("correction.solution.path", contains("Doublons.java"))
                .body("correction.solution[0].content", equalTo("class Doublons { /* solution */ }"))
                .body("correction.explanation", equalTo("Avec un LinkedHashSet."));
        submit(requestId, cardId).statusCode(200).body("rating", equalTo("GOOD"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exercise-failed"))
    void shouldRateAFailedExerciseAgain() {
        runner.willReturn(FakeCodeRunner.FAILED);

        submit(UUID.randomUUID(), exerciseId())
                .statusCode(201)
                .body("rating", equalTo("AGAIN"))
                .body("correction.exerciseRun.tests[0].passed", equalTo(false));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exercise-mismatch"))
    void shouldRefuseARatingForAnExerciseAndFilesForAnotherCard() {
        given().contentType(ContentType.JSON)
                .body(Map.of("requestId", UUID.randomUUID(), "cardId", exerciseId(), "rating", "GOOD"))
                .post(REVIEWS)
                .then()
                .statusCode(422)
                .body("code", equalTo("review.answer-mismatch"));
        submit(UUID.randomUUID(), flashcardId()).statusCode(422);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exercise-unavailable"))
    void shouldRecordNothingWhenTheRunnerIsUnavailable() {
        String cardId = exerciseId();
        UUID requestId = UUID.randomUUID();
        runner.willBeUnavailable();

        submit(requestId, cardId).statusCode(503).body("code", equalTo("review.runner-unavailable"));

        runner.willReturn(FakeCodeRunner.PASSED);
        submit(requestId, cardId).statusCode(201);
    }

    private static ValidatableResponse exercise() {
        return given().queryParam("subject", "elixir")
                .queryParam("type", "CODING_EXERCISE")
                .get(QUEUE)
                .then()
                .statusCode(200);
    }

    private static String exerciseId() {
        return exercise().extract().path("cards[0].cardId");
    }

    private static String flashcardId() {
        return given().queryParam("subject", "elixir")
                .queryParam("type", "FLASHCARD")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .extract()
                .path("cards[0].cardId");
    }

    private static List<Map<String, String>> files(String code) {
        return List.of(Map.of("path", "Doublons.java", "content", code));
    }

    private static ValidatableResponse submit(UUID requestId, String cardId) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("requestId", requestId, "cardId", cardId, "submittedFiles", files(MY_CODE)))
                .post(REVIEWS)
                .then();
    }
}
