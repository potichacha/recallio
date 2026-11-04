package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
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
                        lesson: Les bases se lisent avant tout.
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

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "track-lesson"))
    void shouldShowTheLessonOfANotionBeforeItsFirstCardOnlyToLearn() {
        follow();
        prompts().body("cards[0].lesson", nullValue()).body("cards[1].notionTitle", equalTo("Les bases"));

        chooseLearning();

        prompts()
                .body("cards[0].prompt", equalTo("Les bases ?"))
                .body("cards[0].lesson", equalTo("Les bases se lisent avant tout."));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "track-path"))
    void shouldMapEachNotionWithItsStatusForTheUser() {
        follow();
        chooseLearning();
        path().body("track", equalTo("LEARN"))
                .body("chapters.title", contains("Tout"))
                .body("chapters[0].notions.slug", contains("bases", "avance"))
                .body("chapters[0].notions.status", contains("AVAILABLE", "LOCKED"))
                .body("chapters[0].notions.mastery", contains("NEW", "NEW"));

        String basics = prompts().extract().path("cards[0].cardId");
        given().contentType(ContentType.JSON)
                .body(Map.of("requestId", UUID.randomUUID(), "cardId", basics, "rating", "GOOD"))
                .post("/api/v1/reviews")
                .then()
                .statusCode(201);

        // Une notion d'une seule carte : la réussir l'acquiert, et la maîtriser demande cette carte stable.
        path().body("chapters[0].notions.status", contains("ACQUIRED", "AVAILABLE"))
                .body("chapters[0].notions[0].seenCount", equalTo(1))
                .body("chapters[0].notions.mastery", contains("ACQUIRED", "NEW"))
                .body("chapters[0].notions[0].nextLevel.level", equalTo("MASTERED"))
                .body("chapters[0].notions[0].nextLevel.cards", equalTo(0))
                .body("chapters[0].notions[0].nextLevel.cardsNeeded", equalTo(1))
                .body("chapters[0].notions[0].nextLevel.formatsNeeded", equalTo(1))
                .body("chapters[0].notions[1].nextLevel.level", equalTo("DISCOVERED"));
        given().get("/api/v1/learning-paths/inconnu").then().statusCode(404);
    }

    private static ValidatableResponse path() {
        return given().get("/api/v1/learning-paths/zig").then().statusCode(200);
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
