package com.recallio.review.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Le compte à rebours avant un entretien (ADR 0066). Freya est un sujet en dossier : deux notions essentielles et une
 * courante, d'une carte chacune. Java, décrit par un document Markdown, n'a pas de notion. Les profils des tests sont en
 * UTC, et l'horloge des tests est à midi du jour : « aujourd'hui » est la date UTC du jour.
 */
@QuarkusTest
class InterviewsTest {

    private static final String INTERVIEWS = "/api/v1/me/interviews";
    private static final String FREYA = INTERVIEWS + "/freya";
    private static final String YES_OR_NO = "choices: [{text: Oui, correct: true}, {text: Non, correct: false}]";

    /** Trois semaines : deux pour acquérir les notions, la dernière pour réviser. */
    private static final int THREE_WEEKS = 21;

    @Inject
    ContentImportService contentImport;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubjects() throws IOException {
        try (InputStream input =
                Objects.requireNonNull(getClass().getResourceAsStream("/content/reference-sample.md"))) {
            contentImport.importContent(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        contentImport.importFolder(new SubjectFolder(
                """
                subject: freya
                title: Freya
                category: languages
                position: 980
                status: published
                """,
                Map.of("01-langage.yaml", """
                        domain: langage
                        title: Le langage
                        position: 10
                        chapters:
                          - id: tout
                            title: Tout
                            notions:
                              - {id: bases, title: "Les bases", level: beginner, priority: 1}
                              - {id: types, title: "Les types", level: beginner, priority: 1}
                              - {id: avance, title: "L'avancé", level: expert, priority: 2}
                        """),
                Map.of(
                        "langage/bases.yaml",
                        notion("bases"),
                        "langage/types.yaml",
                        notion("types"),
                        "langage/avance.yaml",
                        notion("avance"))));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(INTERVIEWS).then().statusCode(401);
        given().contentType(ContentType.JSON).body(date(1)).put(FREYA).then().statusCode(401);
        given().delete(FREYA).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "interview-lifecycle"))
    void shouldPlanReadChangeThenRemoveTheDateOfAnInterview() {
        follow("freya");

        plan(THREE_WEEKS)
                .statusCode(200)
                .body("subjectSlug", equalTo("freya"))
                .body("subjectTitle", equalTo("Freya"))
                .body("date", equalTo(today().plusDays(THREE_WEEKS).toString()))
                .body("daysLeft", is(THREE_WEEKS))
                .body("status", equalTo("ON_TRACK"))
                .body("readinessPercent", is(0))
                .body("week", is(1))
                .body("weeks", is(3))
                .body("reviewWeek", is(false))
                // Deux notions sur deux semaines : une cette semaine, l'autre la suivante, puis la révision.
                .body("thisWeek.startsOn", equalTo(today().toString()))
                .body("thisWeek.endsOn", equalTo(today().plusDays(6).toString()))
                .body("thisWeek.quota", is(1))
                .body("thisWeek.notions.slug", contains("bases"))
                .body("thisWeek.notions[0].title", equalTo("Les bases"))
                .body("thisWeek.notions[0].mastery", equalTo("NEW"))
                .body("nextWeeks.number", contains(2, 3))
                .body("nextWeeks.notions", contains(1, 0))
                .body("nextWeeks.review", contains(false, true))
                .body("essentialsLeft", is(2))
                .body("essentials", is(2));

        given().get(INTERVIEWS)
                .then()
                .statusCode(200)
                .body("subjectSlug", contains("freya"))
                .body("[0].date", equalTo(today().plusDays(THREE_WEEKS).toString()));

        plan(60).statusCode(200).body("daysLeft", is(60)).body("weeks", is(9));
        given().get(INTERVIEWS)
                .then()
                .body("[0].date", equalTo(today().plusDays(60).toString()));

        given().delete(FREYA).then().statusCode(204);
        given().get(INTERVIEWS).then().statusCode(200).body("size()", is(0));
        // Retirer une date qui n'existe plus ne change rien.
        given().delete(FREYA).then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "interview-invalid-date"))
    void shouldRefuseADateThatIsNotAfterTodayOrMoreThanAYearAhead() {
        follow("freya");

        plan(0).statusCode(422).body("code", equalTo("interview.invalid-date"));
        plan(-1).statusCode(422).body("code", equalTo("interview.invalid-date"));
        plan(366).statusCode(422).body("code", equalTo("interview.invalid-date"));
        plan(365).statusCode(200);
        given().contentType(ContentType.JSON)
                .body("{}")
                .put(FREYA)
                .then()
                .statusCode(400)
                .body("code", equalTo("validation.failed"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "interview-not-followed"))
    void shouldRefuseASubjectThatIsNotFollowed() {
        follow("java");

        plan(THREE_WEEKS).statusCode(422).body("code", equalTo("interview.subject-not-followed"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "interview-no-essentials"))
    void shouldRefuseASubjectWithoutEssentialNotions() {
        follow("java");

        given().contentType(ContentType.JSON)
                .body(date(THREE_WEEKS))
                .put(INTERVIEWS + "/java")
                .then()
                .statusCode(422)
                .body("code", equalTo("interview.no-essential-notions"));
    }

    /** Arrêter de suivre le sujet garde la date, sans la montrer ; elle revient si on le suit de nouveau. */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "interview-unfollowed"))
    void shouldHideTheDateOfASubjectNoLongerFollowedAndShowItAgainOnceFollowed() {
        follow("freya");
        plan(THREE_WEEKS).statusCode(200);

        follow("java");
        given().get(INTERVIEWS).then().statusCode(200).body("size()", is(0));

        follow("java", "freya");
        given().get(INTERVIEWS).then().statusCode(200).body("subjectSlug", contains("freya"));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "interview-someone-else"))
    void shouldNotShowTheInterviewOfAnotherUser() {
        UUID alice = UUID.randomUUID();
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                                insert into interview_plan
                                    (user_id, subject_slug, interview_on, planned_on, notions_to_acquire)
                                values (:alice, 'freya', current_date + 21, current_date, 2)
                                """)
                        .setParameter("alice", alice)
                        .executeUpdate());
        follow("freya");

        given().get(INTERVIEWS).then().statusCode(200).body("size()", is(0));
        given().delete(FREYA).then().statusCode(204);
        QuarkusTransaction.requiringNew().run(() -> {
            Number left = (Number) entityManager
                    .createNativeQuery("select count(*) from interview_plan where user_id = :alice")
                    .setParameter("alice", alice)
                    .getSingleResult();
            assertThat(left.longValue()).isOne();
        });
    }

    private static ValidatableResponse plan(int daysFromToday) {
        return given().contentType(ContentType.JSON)
                .body(date(daysFromToday))
                .put(FREYA)
                .then();
    }

    private static String date(int daysFromToday) {
        return "{\"date\": \"%s\"}".formatted(today().plusDays(daysFromToday));
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private static String notion(String id) {
        return """
                notion: %s
                cards:
                  - {key: q, format: course, question: "%s, en Freya ?", answer: "Oui.", %s}
                """.formatted(id, id, YES_OR_NO);
    }

    private static void follow(String... subjects) {
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"%s\"]}".formatted(String.join("\", \"", subjects)))
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }
}
