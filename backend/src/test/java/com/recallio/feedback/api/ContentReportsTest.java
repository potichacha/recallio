package com.recallio.feedback.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.feedback.application.ContentReportService;
import com.recallio.feedback.domain.ReportReason;
import com.recallio.feedback.domain.ReportTarget.CardTarget;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Signaler une erreur dans une carte ou une page du Tour, puis la traiter (ADR 0042). */
@QuarkusTest
class ContentReportsTest {

    private static final String REPORTS = "/api/v1/content-reports";
    private static final String ADMIN_REPORTS = "/api/v1/admin/content-reports";

    private static final String SUBJECT = """
            subject: nim
            title: Nim
            category: languages
            position: 960
            status: published
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: premiers-pas
                title: Premiers pas
                notions:
                  - {id: bonjour, title: "Le premier programme", level: beginner, priority: 1}
                  - {id: hors-tour, title: "Une notion sans page", level: beginner, priority: 1}
            """;

    /** Deux pages. */
    private static final String BONJOUR = """
            notion: bonjour
            tour:
              - text: Le premier programme.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
              - text: Encore une page.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            cards:
              - {key: cours, format: course, question: "Où commence un programme Nim ?", answer: "Au début."}
            """;

    private static final String HORS_TOUR = """
            notion: hors-tour
            cards:
              - {key: cours, format: course, question: "Cette notion a-t-elle une page ?", answer: "Non."}
            """;

    @Inject
    ContentImportService contentImport;

    @Inject
    ContentReportService reports;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubject() {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT,
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of("bases/bonjour.yaml", BONJOUR, "bases/hors-tour.yaml", HORS_TOUR)));
    }

    @Test
    void shouldRequireAuthentication() {
        report("{\"cardId\": \"%s\", \"reason\": \"TYPO\"}".formatted(UUID.randomUUID()))
                .statusCode(401);
        given().get(ADMIN_REPORTS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "report-card"))
    void shouldReportACardOnlyOnceWhileTheReportIsOpen() {
        String body = "{\"cardId\": \"%s\", \"reason\": \"WRONG_ANSWER\", \"comment\": \" La réponse est ailleurs. \"}"
                .formatted(cardId("Où commence un programme Nim ?"));

        report(body).statusCode(201).body("reason", is("WRONG_ANSWER"));
        report(body).statusCode(409).body("code", is("feedback.report-already-open"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "report-page"))
    void shouldReportAPageOfTheTourThatExists() {
        report(page("bonjour", 2)).statusCode(201).body("reason", is("UNCLEAR"));
        report(page("bonjour", 3)).statusCode(404).body("code", is("feedback.target-not-found"));
        report(page("hors-tour", 1)).statusCode(404);
        report("{\"cardId\": \"%s\", \"reason\": \"TYPO\"}".formatted(UUID.randomUUID()))
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "report-invalid"))
    void shouldRefuseAReportWithoutExactlyOneTarget() {
        String card = cardId("Où commence un programme Nim ?").toString();

        report("{\"reason\": \"TYPO\"}").statusCode(422).body("code", is("feedback.invalid-target"));
        report("{\"cardId\": \"%s\", \"subject\": \"nim\", \"reason\": \"TYPO\"}".formatted(card))
                .statusCode(422);
        report("{\"subject\": \"nim\", \"notion\": \"bonjour\", \"reason\": \"TYPO\"}")
                .statusCode(422);
        report("{\"cardId\": \"%s\"}".formatted(card)).statusCode(400);
        report("{\"cardId\": \"%s\", \"reason\": \"TYPO\", \"comment\": \"%s\"}".formatted(card, "x".repeat(1001)))
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "report-flood"))
    void shouldMakeAUserWaitBeyondTwentyOpenReports() {
        String sub = "report-flood";
        report(page("bonjour", 1)).statusCode(201);
        QuarkusTransaction.requiringNew().run(() -> {
            UUID userId = (UUID) entityManager
                    .createNativeQuery("select id from app_user where oidc_subject = :sub")
                    .setParameter("sub", sub)
                    .getSingleResult();
            for (int i = 1; i < 20; i++) {
                entityManager
                        .createNativeQuery("""
                                insert into content_report (id, user_id, subject_slug, card_id, reason, comment, created_at)
                                values (:id, :userId, 'nim', :cardId, 'TYPO', '', now())
                                """)
                        .setParameter("id", UUID.randomUUID())
                        .setParameter("userId", userId)
                        .setParameter("cardId", UUID.randomUUID())
                        .executeUpdate();
            }
        });

        report(page("bonjour", 2)).statusCode(429).body("code", is("feedback.too-many-open-reports"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void shouldListTheOpenReportsAndResolveThemOnce() {
        UUID cardId = cardId("Cette notion a-t-elle une page ?");
        String id = reports.report(
                        new UserRef(UUID.randomUUID(), ZoneOffset.UTC),
                        new CardTarget(cardId),
                        ReportReason.TYPO,
                        "Une faute.")
                .id()
                .toString();

        given().get(ADMIN_REPORTS)
                .then()
                .statusCode(200)
                .body("items.id", hasItem(id))
                .body(item(id) + ".subject", is("nim"))
                .body(item(id) + ".cardId", is(cardId.toString()))
                .body(item(id) + ".notionTitle", is("Une notion sans page"))
                .body(item(id) + ".excerpt", is("Cette notion a-t-elle une page ?"))
                .body(item(id) + ".comment", is("Une faute."))
                .body(item(id) + ".resolution", nullValue());

        resolve(id, "FIXED").statusCode(200).body("resolution", is("FIXED"));
        resolve(id, "REJECTED").statusCode(409).body("code", is("feedback.report-already-resolved"));
        resolve(UUID.randomUUID().toString(), "FIXED").statusCode(404);

        given().get(ADMIN_REPORTS).then().statusCode(200).body("items.id", not(hasItem(id)));
        given().queryParam("status", "RESOLVED")
                .get(ADMIN_REPORTS)
                .then()
                .statusCode(200)
                .body("items.id", hasItem(id));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "report-not-admin"))
    void shouldKeepTheReportsToAdmins() {
        given().get(ADMIN_REPORTS).then().statusCode(403);
        resolve(UUID.randomUUID().toString(), "FIXED").statusCode(403);
    }

    private static ValidatableResponse report(String body) {
        return given().contentType(ContentType.JSON).body(body).post(REPORTS).then();
    }

    private static ValidatableResponse resolve(String id, String resolution) {
        return given().contentType(ContentType.JSON)
                .body("{\"resolution\": \"%s\"}".formatted(resolution))
                .put(ADMIN_REPORTS + "/" + id + "/resolution")
                .then();
    }

    private static String page(String notion, int page) {
        return "{\"subject\": \"nim\", \"notion\": \"%s\", \"page\": %d, \"reason\": \"UNCLEAR\"}"
                .formatted(notion, page);
    }

    private static String item(String id) {
        return "items.find { it.id == '%s' }".formatted(id);
    }

    private UUID cardId(String prompt) {
        return QuarkusTransaction.requiringNew().call(() -> (UUID) entityManager
                .createNativeQuery("""
                                select c.id from card c join card_version v on v.card_id = c.id
                                where v.prompt = :prompt and c.retired_at is null
                                """)
                .setParameter("prompt", prompt)
                .getSingleResult());
    }
}
