package com.recallio.gamification.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.recallio.content.application.ContentImportService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * En test : 10 XP pour une question de cours, objectif de 2 cartes, bonus de 50 XP (application.properties ;
 * ADR 0035).
 */
@QuarkusTest
class ProgressResourceTest {

    private static final String PROGRESS = "/api/v1/me/progress";
    private static final String QUEUE = "/api/v1/review-queue";
    private static final int DAILY_GOAL = 2;

    @Inject
    ContentImportService contentImport;

    @Inject
    EntityManager entityManager;

    /** L'horloge de l'application (midi UTC en test) : les jours écrits en base doivent la suivre. */
    @Inject
    Clock clock;

    /** Deux sujets publiés : Java (l'exemple de référence) et un petit document Go, qui a un QCM. */
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
        given().get(PROGRESS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-new-user"))
    void shouldStartFromZero() {
        progress()
                .body("totalXp", is(0))
                .body("subjects", empty())
                .body("streak", is(0))
                .body("cardsToday", is(0))
                .body("dailyGoal", is(DAILY_GOAL))
                .body("goalReachedToday", is(false));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-goal"))
    void shouldAwardXpAndStartTheStreakWhenTheDailyGoalIsReached() {
        List<String> cards = flashcardIds();

        review(UUID.randomUUID(), cards.get(0));
        progress().body("totalXp", is(10)).body("cardsToday", is(1)).body("streak", is(0));

        review(UUID.randomUUID(), cards.get(1));
        progress()
                .body("totalXp", is(10 + 10 + 50))
                .body("cardsToday", is(2))
                .body("goalReachedToday", is(true))
                .body("streak", is(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-replay"))
    void shouldNotAwardXpTwiceForAReplayedReview() {
        UUID requestId = UUID.randomUUID();
        String card = flashcardIds().getFirst();

        review(requestId, card).statusCode(201);
        review(requestId, card).statusCode(200);

        progress().body("totalXp", is(10)).body("cardsToday", is(1));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-subjects"))
    void shouldGiveTheXpAndTheLevelOfEachFollowedSubject() {
        follow("java", "go");

        review(UUID.randomUUID(), flashcardIds().getFirst());
        // Le QCM de Go, juste : son XP de base et un bonus, puis celui de l'objectif du jour, qui n'a pas de sujet.
        answer(goMultipleChoiceId(), "[1]").statusCode(201).body("rating", equalTo("GOOD"));

        progress()
                .body("totalXp", is(10 + (10 + 5) + 50))
                .body("subjects.slug", contains("java", "go"))
                .body("subjects.xp", contains(10, 15))
                .body("subjects.level", contains(1, 1))
                .body("subjects.levelXp", contains(0, 0))
                .body("subjects.nextLevelXp", contains(100, 100));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-freeze-earned"))
    void shouldEarnAStreakFreezeWhenTheGoalIsReachedTheSeventhDayInARow() {
        LocalDate today = LocalDate.now(clock.withZone(myTimezone()));
        goalReached(today.minusDays(6), today.minusDays(1));
        List<String> cards = flashcardIds();

        review(UUID.randomUUID(), cards.get(0));
        progress().body("streak", is(6)).body("streakFreezes", is(0)).body("daysToNextFreeze", is(1));

        review(UUID.randomUUID(), cards.get(1));
        progress()
                .body("streak", is(7))
                .body("streakFreezes", is(1))
                .body("freezeEarnedToday", is(true))
                .body("daysToNextFreeze", is(7));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-freeze-used"))
    void shouldCoverAMissedDayWithAStreakFreeze() {
        // L'objectif atteint les sept jours qui finissent avant-hier, puis hier manqué : le gel gagné le couvre.
        LocalDate today = LocalDate.now(clock.withZone(myTimezone()));
        goalReached(today.minusDays(8), today.minusDays(2));

        progress().body("streak", is(7)).body("streakFreezes", is(0)).body("freezeEarnedToday", is(false));
        given().queryParam("days", 3)
                .get(PROGRESS + "/activity")
                .then()
                .statusCode(200)
                .body("goalReached", contains(true, false, false))
                .body("frozen", contains(false, true, false));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "progress-activity"))
    void shouldListTheRecentDaysIncludingThoseWithoutReviews() {
        review(UUID.randomUUID(), flashcardIds().getFirst());

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

    /** L'objectif atteint chaque jour de {@code from} à {@code to} inclus, écrit directement en base. */
    private void goalReached(LocalDate from, LocalDate to) {
        UUID userId = UUID.fromString(given().get("/api/v1/me").then().extract().path("id"));
        QuarkusTransaction.requiringNew()
                .run(() -> from.datesUntil(to.plusDays(1))
                        .forEach(day -> entityManager
                                .createNativeQuery("""
                                        insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                                        values (:userId, :day, :cards, true)
                                        """)
                                .setParameter("userId", userId)
                                .setParameter("day", day)
                                .setParameter("cards", DAILY_GOAL)
                                .executeUpdate()));
    }

    private static ZoneId myTimezone() {
        return ZoneId.of(given().get("/api/v1/me").then().extract().path("timezone"));
    }

    private static ValidatableResponse progress() {
        return given().get(PROGRESS).then().statusCode(200);
    }

    /** Les deux questions de cours du thème Java de l'exemple : 10 XP chacune. */
    private static List<String> flashcardIds() {
        return given().queryParam("theme", "java-fondamentaux")
                .queryParam("type", "FLASHCARD")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .body("cards.size()", equalTo(2))
                .extract()
                .path("cards.cardId");
    }

    /** Le QCM du document Go de test : sa bonne réponse est la deuxième. */
    private static String goMultipleChoiceId() {
        return given().queryParam("subject", "go")
                .queryParam("type", "MULTIPLE_CHOICE")
                .get(QUEUE)
                .then()
                .statusCode(200)
                .extract()
                .path("cards[0].cardId");
    }

    private static void follow(String... subjects) {
        String list = String.join("\", \"", subjects);
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"%s\"]}".formatted(list))
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }

    private static ValidatableResponse review(UUID requestId, String cardId) {
        return given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"rating\": \"GOOD\"}".formatted(requestId, cardId))
                .post("/api/v1/reviews")
                .then();
    }

    private static ValidatableResponse answer(String cardId, String selectedChoices) {
        return given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"selectedChoices\": %s}"
                        .formatted(UUID.randomUUID(), cardId, selectedChoices))
                .post("/api/v1/reviews")
                .then();
    }
}
