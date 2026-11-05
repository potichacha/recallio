package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** L'essai sans compte (ADR 0050) : aucun test ne se connecte, c'est tout l'intérêt. */
@QuarkusTest
class TrialResourceTest {

    private static final String TRIAL = "/api/v1/subjects/{subject}/trial";

    @Inject
    ContentImportService contentImport;

    /**
     * Un sujet en dossier : deux notions essentielles (une débutante, une intermédiaire), une avancée et une question de
     * cours, qui ne font pas partie de l'essai ; plus un document Markdown, sans notions.
     */
    @BeforeEach
    void importSubjects() throws IOException {
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
                          - id: valeurs
                            title: Valeurs
                            notions:
                              - {id: atomes, title: "Les atomes", level: intermediate, priority: 1}
                              - {id: immuable, title: "L'immuabilité", level: beginner, priority: 1}
                              - {id: macros, title: "Les macros", level: expert, priority: 3}
                        """),
                Map.of(
                        "bases/atomes.yaml", """
                        notion: atomes
                        cards:
                          - key: atome
                            format: multiple-choice
                            question: "Lequel est un atome ?"
                            choices:
                              - text: ":ok"
                                correct: true
                              - text: "ok"
                                correct: false
                            answer: "Un atome commence par deux-points."
                        """,
                        "bases/immuable.yaml", """
                        notion: immuable
                        cards:
                          - {key: cours, format: flashcard, question: "Qu'est-ce que l'immuabilité ?", answer: "Rien ne change."}
                          - {key: reaffecter, format: true-false, question: "Une donnée Elixir se modifie.", truth: false, answer: "Non."}
                        """,
                        "bases/macros.yaml", """
                        notion: macros
                        cards:
                          - {key: macro, format: true-false, question: "Une macro s'exécute à la compilation.", truth: true, answer: "Oui."}
                        """)));
        try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream("/content/go-sample.md"))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void shouldOfferTheEssentialNotionsWithoutAnAccountFromTheSimplestOne() {
        given().get(TRIAL, "elixir")
                .then()
                .statusCode(200)
                .body("questions", hasSize(2))
                .body("questions.notion", contains("immuable", "atomes"))
                .body("questions[0].card.type", equalTo("TRUE_FALSE"))
                .body("questions[0].card.choices", contains("Vrai", "Faux"))
                .body("questions[0].card.answer", nullValue());
    }

    @Test
    void shouldGradeAnAnswerWithoutAnAccountAndRecordNothing() {
        String cardId = given().get(TRIAL, "elixir").then().extract().path("questions[1].card.cardId");

        given().contentType(ContentType.JSON)
                .body("{\"cardId\": \"%s\", \"selectedChoices\": [0]}".formatted(cardId))
                .post(TRIAL + "/{notion}/checks", "elixir", "atomes")
                .then()
                .statusCode(200)
                .body("correct", equalTo(true))
                .body("explanation", equalTo("Un atome commence par deux-points."));
    }

    @Test
    void shouldKeepTheTrialOfADraftSubjectHidden() {
        contentImport.importFolder(
                new SubjectFolder("""
                subject: erlang
                title: Erlang
                category: languages
                position: 940
                status: draft
                """, Map.of("01-bases.yaml", """
                        domain: bases
                        title: Les bases
                        position: 10
                        chapters:
                          - id: processus
                            title: Processus
                            notions:
                              - {id: processus-legers, title: "Les processus", level: beginner, priority: 1}
                        """), Map.of("bases/processus-legers.yaml", """
                        notion: processus-legers
                        cards:
                          - {key: leger, format: true-false, question: "Un processus Erlang est un thread du système.", truth: false, answer: "Non."}
                        """)));

        given().get(TRIAL, "erlang").then().statusCode(404);
    }

    @Test
    void shouldHaveNoTrialForASubjectWithoutNotions() {
        given().get(TRIAL, "go").then().statusCode(404).body("code", equalTo("review.trial-unavailable"));
        given().get(TRIAL, "inconnu").then().statusCode(404);
    }
}
