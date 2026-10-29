package com.recallio.gamification.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.recallio.content.application.ContentImportService;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** En test : 10 points par carte, objectif de 2 cartes, bonus de 50 points (application.properties). */
@QuarkusTest
class ProgressResourceTest {

    private static final String PROGRESS = "/api/v1/me/progress";

    @Inject
    ContentImportService contentImport;

    @BeforeEach
    void importSample() throws IOException {
        try (InputStream input =
                Objects.requireNonNull(getClass().getResourceAsStream("/content/reference-sample.md"))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(PROGRESS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-new-user"))
    void shouldStartFromZero() {
        progress()
                .body("totalPoints", is(0))
                .body("streak", is(0))
                .body("cardsToday", is(0))
                .body("dailyGoal", is(2))
                .body("goalReachedToday", is(false));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-goal"))
    void shouldAwardPointsAndStartTheStreakWhenTheDailyGoalIsReached() {
        List<String> cards = cardIds();

        review(UUID.randomUUID(), cards.get(0));
        progress().body("totalPoints", is(10)).body("cardsToday", is(1)).body("streak", is(0));

        review(UUID.randomUUID(), cards.get(1));
        progress()
                .body("totalPoints", is(10 + 10 + 50))
                .body("cardsToday", is(2))
                .body("goalReachedToday", is(true))
                .body("streak", is(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-replay"))
    void shouldNotAwardPointsTwiceForAReplayedReview() {
        UUID requestId = UUID.randomUUID();
        String card = cardIds().getFirst();

        review(requestId, card).statusCode(201);
        review(requestId, card).statusCode(200);

        progress().body("totalPoints", is(10)).body("cardsToday", is(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-activity"))
    void shouldListTheRecentDaysIncludingThoseWithoutReviews() {
        review(UUID.randomUUID(), cardIds().getFirst());

        given().queryParam("days", 3)
                .get(PROGRESS + "/activity")
                .then()
                .statusCode(200)
                .body("size()", is(3))
                .body("[0].cardsReviewed", is(0))
                .body("[2].day", equalTo(LocalDate.now(ZoneOffset.UTC).toString()))
                .body("[2].cardsReviewed", is(1))
                .body("[2].goalReached", is(false));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-activity-bounds"))
    void shouldBoundTheNumberOfDays() {
        given().queryParam("days", 365).get(PROGRESS + "/activity").then().statusCode(400);
    }

    private static ValidatableResponse progress() {
        return given().get(PROGRESS).then().statusCode(200);
    }

    private static List<String> cardIds() {
        return given().queryParam("theme", "java-fondamentaux")
                .get("/api/v1/review-queue")
                .then()
                .statusCode(200)
                .body("cards.size()", equalTo(3))
                .extract()
                .path("cards.cardId");
    }

    private static ValidatableResponse review(UUID requestId, String cardId) {
        return given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"rating\": \"GOOD\"}".formatted(requestId, cardId))
                .post("/api/v1/reviews")
                .then();
    }
}
