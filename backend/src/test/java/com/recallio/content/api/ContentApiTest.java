package com.recallio.content.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ContentApiTest {

    private static final String IMPORTS = "/api/v1/admin/content-imports";
    private static final String JVM_QUESTION =
            "**Q : Qu'est-ce que la **JVM** ?** R : La machine virtuelle qui exécute le bytecode.";

    private static String sample;

    /** Un mini document Go : même numéro de chapitre et même question que l'exemple Java. */
    private static String goDocument(String status) {
        return """
                ---
                subject: go
                title: Go
                category: languages
                position: 20
                status: %s
                ---
                # 1. GO : FONDAMENTAUX
                ### Questions techniques
                %s
                """.formatted(status, JVM_QUESTION);
    }

    @BeforeAll
    static void loadSample() throws IOException {
        try (InputStream input =
                Objects.requireNonNull(ContentApiTest.class.getResourceAsStream("/content/reference-sample.md"))) {
            sample = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nested
    @TestSecurity(user = "admin", roles = "admin")
    class Import {

        @Test
        void shouldBeIdempotent() {
            importContent(sample);

            importContent(sample)
                    .body("cardsAdded", is(0))
                    .body("cardsUpdated", is(0))
                    .body("cardsRetired", is(0))
                    .body("cardsUnchanged", is(8));
        }

        @Test
        void shouldReportSourceIssues() {
            importContent(sample)
                    .body("issues.reason", hasItem("Question sans réponse (ligne « R : » attendue)"))
                    .body("issues.excerpt", hasItem("Une ligne orpheline qui ne correspond à rien."));
        }

        @Test
        void shouldVersionAnEditedAnswerUnderTheSameCard() {
            importContent(sample);
            String edited = sample.replace("qui exécute le bytecode.", "qui exécute le bytecode Java.");

            importContent(edited).body("cardsUpdated", is(1)).body("cardsAdded", is(0));
            given().get("/api/v1/themes/java-fondamentaux/cards")
                    .then()
                    .statusCode(200)
                    .body("items[0].answer", equalTo("La machine virtuelle qui exécute le bytecode Java."))
                    .body("items[0].version", is(2));

            importContent(sample);
        }

        @Test
        void shouldRetireThenReactivateARemovedQuestion() {
            importContent(sample);

            importContent(sample.replace(JVM_QUESTION, "")).body("cardsRetired", is(1));
            given().get("/api/v1/themes/java-fondamentaux").then().body("cardCount", is(3));

            importContent(sample).body("cardsReactivated", is(1));
            given().get("/api/v1/themes/java-fondamentaux").then().body("cardCount", is(4));
        }

        @Test
        void shouldImportEachSubjectWithoutTouchingTheOthers() {
            importContent(sample);

            importContent(goDocument("published"))
                    .body("subject", equalTo("go"))
                    .body("cardsRetired", is(0));

            given().get("/api/v1/themes/java-fondamentaux").then().body("cardCount", is(4));
            given().get("/api/v1/themes/go-fondamentaux")
                    .then()
                    .statusCode(200)
                    .body("subjectSlug", equalTo("go"))
                    .body("cardCount", is(1));
            // Même numéro de chapitre que Java, et même question : deux thèmes, deux cartes distincts.
            importContent(sample).body("cardsUnchanged", is(8)).body("cardsRetired", is(0));
        }

        @Test
        void shouldHideTheThemesOfADraftSubject() {
            importContent(goDocument("draft"));

            given().get("/api/v1/themes/go-fondamentaux").then().statusCode(404);

            importContent(goDocument("published"));
            given().get("/api/v1/themes/go-fondamentaux").then().statusCode(200);
        }

        @Test
        void shouldRejectADocumentWithoutSubjectHeader() {
            given().contentType("text/markdown; charset=UTF-8")
                    .body("# 1. JAVA : FONDAMENTAUX\n")
                    .post(IMPORTS)
                    .then()
                    .statusCode(422)
                    .body("code", equalTo("content.invalid-source"));
        }

        private ValidatableResponse importContent(String markdown) {
            return given().contentType("text/markdown; charset=UTF-8")
                    .body(markdown)
                    .post(IMPORTS)
                    .then()
                    .statusCode(200);
        }
    }

    /** Le catalogue est public ; l'identité admin ne sert qu'à importer la fixture avant chaque test. */
    @Nested
    @TestSecurity(user = "admin", roles = "admin")
    class Browse {

        @org.junit.jupiter.api.BeforeEach
        void importSample() {
            given().contentType("text/markdown; charset=UTF-8")
                    .body(sample)
                    .post(IMPORTS)
                    .then()
                    .statusCode(200);
        }

        @Test
        void shouldListThemesInProgramOrderWithTheirCardCount() {
            given().get("/api/v1/themes")
                    .then()
                    .statusCode(200)
                    .body("slug", hasItem("java-fondamentaux"))
                    .body("find { it.slug == 'java-glossaire-des-sigles-et-termes' }.cardCount", is(2))
                    .body("find { it.slug == 'java-fondamentaux' }.subjectSlug", equalTo("java"))
                    .body("find { it.slug == 'java-fondamentaux' }.subjectTitle", equalTo("Java"));
        }

        @Test
        void shouldPaginateCardsOfATheme() {
            given().queryParam("size", 2)
                    .get("/api/v1/themes/java-fondamentaux/cards")
                    .then()
                    .statusCode(200)
                    .body("items.type", contains("FLASHCARD", "FLASHCARD"))
                    .body("size", is(2))
                    .body("totalPages", is(2));
        }

        @Test
        void shouldAnswer404WithAProblemDetailForAnUnknownTheme() {
            given().get("/api/v1/themes/inconnu/cards")
                    .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("code", equalTo("content.theme-not-found"))
                    .body("status", is(404));
        }

        @Test
        void shouldRejectAnOversizedPage() {
            given().queryParam("size", 1000)
                    .get("/api/v1/themes/java-fondamentaux/cards")
                    .then()
                    .statusCode(400)
                    .contentType("application/problem+json")
                    .body("code", equalTo("validation.failed"))
                    .body("invalidParams.name", hasItem("size"));
        }
    }

    @Nested
    class Security {

        @Test
        void shouldRequireAuthenticationToImport() {
            given().contentType(ContentType.TEXT).body("x").post(IMPORTS).then().statusCode(401);
        }

        @Test
        @TestSecurity(user = "alice", roles = "user")
        void shouldRequireTheAdminRoleToImport() {
            given().contentType(ContentType.TEXT).body("x").post(IMPORTS).then().statusCode(403);
        }
    }
}
