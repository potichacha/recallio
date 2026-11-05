package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.review.application.MockExamService;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** L'examen blanc (ADR 0044) : des questions corrigées par le serveur, une note, le corrigé, et rien dans FSRS. */
@QuarkusTest
class MockExamsTest {

    private static final String EXAMS = "/api/v1/me/mock-exams";

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: tout
                title: Tout
                notions:
                  - {id: types, title: "Les types", level: beginner, priority: 1}
                  - {id: fonctions, title: "Les fonctions", level: beginner, priority: 2}
            """;

    /** Cinq questions corrigées par le serveur (les vrais ou faux sont faux, le QCM est le premier choix, le trou « int »), et un cours. */
    private static final String TYPES = """
            notion: types
            cards:
              - {key: cours, format: course, question: "Qu'est-ce qu'un type ?", answer: "Une famille de valeurs."}
              - {key: vf1, format: true-false, question: "Dart n'a pas de types.", truth: false, answer: "Il en a."}
              - {key: vf2, format: true-false, question: "Un int peut valoir 1.5.", truth: false, answer: "Non."}
              - key: choix
                format: multiple-choice
                question: "Quel type pour un entier ?"
                choices:
                  - {text: "`int`", correct: true}
                  - {text: "`String`", correct: false}
                answer: "`int`."
            """;

    private static final List<String> TYPES_PROMPTS =
            List.of("Dart n'a pas de types.", "Un int peut valoir 1.5.", "Quel type pour un entier ?");

    private static final List<String> FONCTIONS_PROMPTS =
            List.of("`___ carre(int x) => x * x;`", "Une fonction Dart ne rend jamais rien.");

    /** Un seul domaine, une seule notion : trois questions corrigées par le serveur, trop peu pour un examen. */
    private static final String ELM_REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: tout
                title: Tout
                notions:
                  - {id: types, title: "Les types", level: beginner, priority: 1}
            """;

    private static final String FONCTIONS = """
            notion: fonctions
            cards:
              - {key: trou, format: fill-in, question: "`___ carre(int x) => x * x;`", blanks: [[int]], answer: "Elle rend un int."}
              - {key: vf, format: true-false, question: "Une fonction Dart ne rend jamais rien.", truth: false, answer: "Elle peut."}
            """;

    @Inject
    ContentImportService contentImport;

    @Inject
    MockExamService exams;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubjects() {
        contentImport.importFolder(new SubjectFolder(
                subject("dart", 980),
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of("bases/types.yaml", TYPES, "bases/fonctions.yaml", FONCTIONS)));
        contentImport.importFolder(new SubjectFolder(
                subject("elm", 990), Map.of("01-bases.yaml", ELM_REFERENTIAL), Map.of("bases/types.yaml", TYPES)));
    }

    @Test
    void shouldRequireAuthentication() {
        start("dart").statusCode(401);
        given().get(EXAMS + "?subject=dart").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exam-taker"))
    void shouldGradeTheCopyThenKeepOnlyTheMark() {
        JsonPath exam = start("dart")
                .statusCode(201)
                .body("subject", is("dart"))
                .body("submitted", is(false))
                .body("questions", hasSize(5))
                .body("questions.type", everyItem(oneOf("TRUE_FALSE", "MULTIPLE_CHOICE", "FILL_IN")))
                .body("remainingMs", greaterThan(4 * 60_000))
                .extract()
                .jsonPath();
        assertThat(exam.prettify()).doesNotContain("correct", "Une famille de valeurs.");
        // Une question par notion à chaque tour, l'essentielle d'abord : « types », puis « fonctions », puis de
        // nouveau.
        List<String> prompts = exam.getList("questions.prompt");
        assertThat(prompts.get(0)).isIn(TYPES_PROMPTS);
        assertThat(prompts.get(1)).isIn(FONCTIONS_PROMPTS);
        assertThat(prompts.get(4)).isIn(TYPES_PROMPTS);

        String id = exam.getString("id");
        List<String> cards = exam.getList("questions.cardId");
        String answers = """
                {"answers": [
                  {"cardId": "%s", "selectedChoices": [1]},
                  {"cardId": "%s", "selectedChoices": [0]}
                ]}""".formatted(cardOf(exam, "Dart n'a pas de types."), cardOf(exam, "Un int peut valoir 1.5."));

        submit(id, answers)
                .statusCode(200)
                .body("correct", is(1))
                .body("total", is(5))
                .body("percent", is(20))
                .body("inTime", is(true))
                .body("results.cardId", contains(cards.toArray()))
                .body(
                        "results.find { it.cardId == '%s' }.blanks[0].expected"
                                .formatted(cardOf(exam, "`___ carre(int x) => x * x;`")),
                        is("int"));
        submit(id, answers).statusCode(409).body("code", is("review.mock-exam-already-submitted"));

        given().get(EXAMS + "/" + id).then().statusCode(200).body("submitted", is(true));
        given().queryParam("subject", "dart")
                .get(EXAMS)
                .then()
                .statusCode(200)
                .body("id", contains(id))
                .body("[0].percent", is(20));
        // Rien n'est passé par la révision.
        assertThat(reviewLogCount()).isZero();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exam-late"))
    void shouldMarkALateCopyOutOfTime() {
        String id = start("dart").statusCode(201).extract().path("id");
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery(
                                "update mock_exam set deadline = started_at - interval '1 hour' where id = :id")
                        .setParameter("id", UUID.fromString(id))
                        .executeUpdate());

        submit(id, "{\"answers\": []}").statusCode(200).body("correct", is(0)).body("inTime", is(false));
        given().get(EXAMS + "/" + id).then().statusCode(200).body("remainingMs", is(0));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "exam-rules"))
    void shouldRefuseASubjectWithTooFewQuestionsAndTheExamsOfOthers() {
        start("elm").statusCode(422).body("code", is("review.not-enough-exam-questions"));
        start("inconnu").statusCode(404);

        UUID someoneElse = exams.start(new UserRef(UUID.randomUUID(), ZoneOffset.UTC), "dart")
                .exam()
                .id();
        given().get(EXAMS + "/" + someoneElse).then().statusCode(404).body("code", is("review.mock-exam-not-found"));
        submit(someoneElse.toString(), "{\"answers\": []}").statusCode(404);
    }

    private static String subject(String slug, int position) {
        return """
                subject: %s
                title: %s
                category: languages
                position: %d
                status: published
                """.formatted(slug, slug.substring(0, 1).toUpperCase() + slug.substring(1), position);
    }

    private static ValidatableResponse start(String subject) {
        return given().contentType(ContentType.JSON)
                .body("{\"subject\": \"%s\"}".formatted(subject))
                .post(EXAMS)
                .then();
    }

    private static ValidatableResponse submit(String id, String body) {
        return given().contentType(ContentType.JSON)
                .body(body)
                .post(EXAMS + "/" + id + "/submission")
                .then();
    }

    private static String cardOf(JsonPath exam, String prompt) {
        return exam.getString("questions.find { it.prompt == '%s' }.cardId".formatted(prompt.replace("'", "\\'")));
    }

    private long reviewLogCount() {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager.createNativeQuery("""
                                        select count(*) from review_log l join app_user u on u.id = l.user_id
                                        where u.oidc_subject = 'exam-taker'
                                        """).getSingleResult()).longValue());
    }
}
