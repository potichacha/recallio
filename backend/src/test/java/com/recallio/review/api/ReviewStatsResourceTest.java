package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.recallio.content.application.ContentImportService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Documents d'exemple : Java (4 cartes dans « java-fondamentaux », 8 au total) et Go (2 cartes) ; quota de 3 nouvelles
 * cartes par jour en test.
 */
@QuarkusTest
class ReviewStatsResourceTest {

    private static final String STATS = "/api/v1/me/review-stats";
    private static final String THEME = "java-fondamentaux";
    private static final String JAVA = "subjects.find { it.slug == 'java' }";
    private static final String JAVA_THEME = JAVA + ".themes.find { it.slug == '%s' }".formatted(THEME);

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importSubjects() throws IOException {
        for (String resource : new String[] {"/content/reference-sample.md", "/content/go-sample.md"}) {
            try (InputStream input = Objects.requireNonNull(getClass().getResourceAsStream(resource))) {
                contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(STATS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "stats-no-subject"))
    void shouldCountNothingUntilTheUserFollowsASubject() {
        given().get(STATS)
                .then()
                .statusCode(200)
                .body("overall.total", is(0))
                .body("newToday", is(0))
                .body("subjects", empty());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "stats-new-user"))
    void shouldShowAllCardsAsNewForANewUser() {
        follow("java");

        given().get(STATS)
                .then()
                .statusCode(200)
                .body("overall.seen", is(0))
                .body("overall.dueNow", is(0))
                .body("newToday", is(3))
                .body("subjects.slug", contains("java"))
                .body(JAVA + ".title", equalTo("Java"))
                .body(JAVA_THEME + ".counts.total", is(4))
                .body(JAVA_THEME + ".title", equalTo("JAVA : FONDAMENTAUX"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "stats-by-subject"))
    void shouldGroupTheFollowedSubjectsInCatalogueOrderWithTheirOwnCounts() {
        follow("go", "java");

        var stats = given().get(STATS)
                .then()
                .statusCode(200)
                .body("subjects.slug", contains("java", "go"))
                .body("subjects.find { it.slug == 'go' }.counts.total", is(2))
                .body("subjects.find { it.slug == 'go' }.themes.slug", contains("go-fondamentaux"))
                .extract()
                .jsonPath();

        assertThat(stats.getInt("overall.total")).isEqualTo(stats.getInt(JAVA + ".counts.total") + 2);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "stats-after-review"))
    void shouldCountReviewedCardsPerSubjectAndThemeAndAsDueToday() {
        follow("java");
        String cardId = given().queryParam("theme", THEME)
                .get("/api/v1/review-queue")
                .then()
                .extract()
                .path("cards[0].cardId");
        given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"rating\": \"AGAIN\"}"
                        .formatted(UUID.randomUUID(), cardId))
                .post("/api/v1/reviews")
                .then()
                .statusCode(201);

        given().get(STATS)
                .then()
                .statusCode(200)
                .body("overall.seen", is(1))
                .body("overall.dueToday", is(1))
                .body("overall.mastered", is(0))
                .body("newToday", is(2))
                .body(JAVA + ".counts.seen", is(1))
                .body(JAVA + ".counts.dueToday", is(1))
                .body(JAVA_THEME + ".counts.seen", is(1));
    }

    private static void follow(String... subjects) {
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"%s\"]}".formatted(String.join("\", \"", subjects)))
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }
}
