package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;

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
 * Les parcours (ADR 0032) changent l'ordre et le choix des nouvelles cartes. Le sujet de test a deux notions : « bases »
 * (débutante, courante), puis « avance » (experte, essentielle), qui requiert « bases ».
 */
@QuarkusTest
class LearningTracksTest {

    private static final String QUEUE = "/api/v1/review-queue";

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importASubjectWithAPrerequisite() {
        contentImport.importFolder(new SubjectFolder(
                """
                subject: zig
                title: Zig
                category: languages
                position: 940
                status: published
                """, Map.of("01-langage.yaml", """
                        domain: langage
                        title: Le langage
                        position: 10
                        chapters:
                          - id: tout
                            title: Tout
                            notions:
                              - {id: bases, title: "Les bases", level: beginner, priority: 2}
                              - {id: avance, title: "L'avancé", level: expert, priority: 1, requires: [bases]}
                        """), Map.of("langage/bases.yaml", """
                        notion: bases
                        cards:
                          - {key: q, format: course, question: "Les bases ?", answer: "Oui."}
                        """, "langage/avance.yaml", """
                        notion: avance
                        cards:
                          - {key: q, format: course, question: "L'avancé ?", answer: "Oui."}
                        """)));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "track-prepare"))
    void shouldOfferTheMostEssentialNotionFirstToPrepareAnInterview() {
        follow();

        prompts().body("cards.prompt", contains("L'avancé ?", "Les bases ?"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "track-learn"))
    void shouldUnlockANotionOnceItsPrerequisiteIsAcquired() {
        follow();
        chooseLearning();

        String basics = prompts()
                .body("cards.prompt", contains("Les bases ?"))
                .extract()
                .path("cards[0].cardId");
        given().contentType(ContentType.JSON)
                .body(Map.of("requestId", UUID.randomUUID(), "cardId", basics, "rating", "GOOD"))
                .post("/api/v1/reviews")
                .then()
                .statusCode(201);

        prompts().body("cards.prompt", contains("L'avancé ?"));
    }

    private static void follow() {
        given().contentType(ContentType.JSON)
                .body(Map.of("subjects", List.of("zig")))
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }

    private static void chooseLearning() {
        given().contentType(ContentType.JSON)
                .body(Map.of("track", "LEARN"))
                .put("/api/v1/me/followed-subjects/zig/track")
                .then()
                .statusCode(200);
    }

    private static ValidatableResponse prompts() {
        return given().get(QUEUE).then().statusCode(200);
    }
}
