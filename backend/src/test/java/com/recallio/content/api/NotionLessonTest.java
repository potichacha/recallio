package com.recallio.content.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** La leçon d'une notion, dans la page Cours (ADR 0052) : publique comme le catalogue, cachée pour un brouillon. */
@QuarkusTest
class NotionLessonTest {

    private static final String LESSON = "/api/v1/subjects/{subject}/notions/{notion}";

    @Inject
    ContentImportService contentImport;

    /** Trois notions, dans l'ordre du référentiel ; seule la deuxième a une leçon. */
    @BeforeEach
    void importSubjects() {
        contentImport.importFolder(subject("haskell", "published"));
        contentImport.importFolder(subject("ocaml", "draft"));
    }

    private static SubjectFolder subject(String slug, String status) {
        return new SubjectFolder(
                """
                subject: %s
                title: Un sujet
                category: languages
                position: 950
                status: %s
                """.formatted(slug, status),
                Map.of("01-bases.yaml", """
                        domain: bases
                        title: Les bases
                        position: 10
                        chapters:
                          - id: valeurs
                            title: Les valeurs
                            notions:
                              - {id: expressions, title: "Les expressions", level: beginner, priority: 1}
                              - {id: fonctions, title: "Les fonctions", level: beginner, priority: 1}
                              - {id: types, title: "Les types", level: beginner, priority: 1}
                        """),
                Map.of(
                        "bases/expressions.yaml", """
                        notion: expressions
                        cards:
                          - {key: vf, format: true-false, question: "Tout est expression.", truth: true, answer: "Oui."}
                        """,
                        "bases/fonctions.yaml", """
                        notion: fonctions
                        lesson: Une fonction **associe** une valeur à chaque argument.
                        cards:
                          - {key: vf, format: true-false, question: "Une fonction est une valeur.", truth: true, answer: "Oui."}
                        """,
                        "bases/types.yaml", """
                        notion: types
                        cards:
                          - {key: vf, format: true-false, question: "Les types sont inférés.", truth: true, answer: "Oui."}
                        """));
    }

    @Test
    void shouldServeTheLessonOfANotionWithItsNeighboursWithoutAnAccount() {
        given().get(LESSON, "haskell", "fonctions")
                .then()
                .statusCode(200)
                .body("title", equalTo("Les fonctions"))
                .body("chapterTitle", equalTo("Les valeurs"))
                .body("lesson", equalTo("Une fonction **associe** une valeur à chaque argument."))
                .body("inTour", equalTo(false))
                .body("previous.slug", equalTo("expressions"))
                .body("next.title", equalTo("Les types"));
    }

    @Test
    void shouldHaveNoLessonYetAndNoNotionBeforeTheFirstOne() {
        given().get(LESSON, "haskell", "expressions")
                .then()
                .statusCode(200)
                .body("lesson", nullValue())
                .body("previous", nullValue())
                .body("next.slug", equalTo("fonctions"));
    }

    @Test
    void shouldAnswer404ForAnUnknownNotionOrADraftSubject() {
        given().get(LESSON, "haskell", "monades")
                .then()
                .statusCode(404)
                .body("code", equalTo("content.notion-not-found"));
        given().get(LESSON, "ocaml", "fonctions").then().statusCode(404);
    }
}
