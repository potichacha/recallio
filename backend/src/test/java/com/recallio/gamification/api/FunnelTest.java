package com.recallio.gamification.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.CardType;
import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.gamification.domain.funnel.FunnelReport;
import com.recallio.gamification.domain.funnel.FunnelStep;
import com.recallio.gamification.domain.leaderboard.Week;
import com.recallio.review.CardReviewed;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Le parcours mesuré (ADR 0064) : chaque étape enregistrée là où elle se produit, sans rien qui désigne le visiteur,
 * et le rapport réservé aux admins. Les tests Quarkus s'exécutent l'un après l'autre : chacun compare le nombre
 * d'événements d'une étape avant et après ce qu'il fait. Le calcul des semaines est couvert par FunnelServiceTest.
 */
@QuarkusTest
class FunnelTest {

    private static final String VISITS = "/api/v1/funnel/visits";
    private static final String FUNNEL = "/api/v1/admin/funnel";
    private static final String TRIAL = "/api/v1/subjects/{subject}/trial";

    @Inject
    Event<CardReviewed> cardReviewed;

    @Inject
    ContentImportService contentImport;

    @Inject
    EntityManager entityManager;

    @Inject
    Clock clock;

    @Test
    void shouldCountAVisitWithoutAnAccount() {
        long before = count(FunnelStep.VISIT);

        given().post(VISITS).then().statusCode(204);

        assertThat(count(FunnelStep.VISIT)).isEqualTo(before + 1);
    }

    @Test
    void shouldCountATrialWhenItsFirstQuestionIsAnswered() {
        importTrialSubject();
        // Charger les questions ne compte pas : la page publique les charge pour chaque visiteur.
        long before = count(FunnelStep.TRIAL);
        JsonPath trial =
                given().get(TRIAL, "ocaml").then().statusCode(200).extract().jsonPath();
        String first = trial.getString("questions[0].card.cardId");
        String second = trial.getString("questions[1].card.cardId");
        assertThat(count(FunnelStep.TRIAL)).isEqualTo(before);

        check("motifs", second);
        assertThat(count(FunnelStep.TRIAL)).isEqualTo(before);

        check("immuable", first);
        assertThat(count(FunnelStep.TRIAL)).isEqualTo(before + 1);
    }

    @Test
    @TestSecurity(user = "carol")
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-funnel-newcomer"))
    void shouldCountAnAccountOnTheFirstLoginOnly() {
        long before = count(FunnelStep.ACCOUNT);

        given().get("/api/v1/me").then().statusCode(200);
        given().get("/api/v1/me").then().statusCode(200);

        assertThat(count(FunnelStep.ACCOUNT)).isEqualTo(before + 1);
    }

    @Test
    void shouldCountTheVeryFirstReviewOfAUserOnce() {
        UserRef newcomer = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        long before = count(FunnelStep.FIRST_REVIEW);

        review(newcomer);
        review(newcomer);

        assertThat(count(FunnelStep.FIRST_REVIEW)).isEqualTo(before + 1);
    }

    @Test
    void shouldNotCountTheFirstReviewOfTheDayOfAUserWhoHasAlreadyReviewed() {
        UserRef regular = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        LocalDate yesterday =
                LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).minusDays(1);
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                                insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                                values (:userId, :day, 3, true)
                                """)
                        .setParameter("userId", regular.id())
                        .setParameter("day", yesterday)
                        .executeUpdate());
        long before = count(FunnelStep.FIRST_REVIEW);

        review(regular);

        assertThat(count(FunnelStep.FIRST_REVIEW)).isEqualTo(before);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void shouldReportTheEightLastWeeksToAnAdmin() {
        given().post(VISITS).then().statusCode(204);

        given().get(FUNNEL)
                .then()
                .statusCode(200)
                .body("weeks", hasSize(FunnelReport.WEEKS))
                .body("weeks[0].steps.step", contains("VISIT", "TRIAL", "ACCOUNT", "FIRST_REVIEW"))
                .body("weeks[0].steps[0].count", greaterThanOrEqualTo(1))
                .body("weeks[0].steps[0].percent", nullValue())
                .body(
                        "weeks[0].weekStart",
                        equalTo(Week.containing(LocalDate.now(clock)).monday().toString()));
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void shouldReserveTheReportToAdmins() {
        given().get(FUNNEL).then().statusCode(403);
    }

    @Test
    void shouldRequireAuthenticationForTheReport() {
        given().get(FUNNEL).then().statusCode(401);
    }

    private void check(String notion, String cardId) {
        given().contentType(ContentType.JSON)
                .body("{\"cardId\": \"%s\", \"selectedChoices\": [0]}".formatted(cardId))
                .post(TRIAL + "/{notion}/checks", "ocaml", notion)
                .then()
                .statusCode(200);
    }

    private void review(UserRef user) {
        QuarkusTransaction.requiringNew()
                .run(() -> cardReviewed.fire(new CardReviewed(
                        user,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "java",
                        CardType.TRUE_FALSE,
                        true,
                        true,
                        clock.instant())));
    }

    private long count(FunnelStep step) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery("select count(*) from funnel_event where step = :step")
                                .setParameter("step", step.name())
                                .getSingleResult())
                        .longValue());
    }

    /** Un sujet en dossier dont l'essai pose deux questions : « immuable » (débutante) puis « motifs ». */
    private void importTrialSubject() {
        contentImport.importFolder(new SubjectFolder(
                """
                subject: ocaml
                title: OCaml
                category: languages
                position: 950
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
                              - {id: motifs, title: "Le filtrage par motif", level: intermediate, priority: 1}
                              - {id: immuable, title: "L'immuabilité", level: beginner, priority: 1}
                        """),
                Map.of(
                        "bases/immuable.yaml", """
                        notion: immuable
                        cards:
                          - {key: reaffecter, format: true-false, question: "Une valeur OCaml se modifie.", truth: false, answer: "Non."}
                        """,
                        "bases/motifs.yaml", """
                        notion: motifs
                        cards:
                          - {key: exhaustif, format: true-false, question: "Le compilateur signale un filtrage incomplet.", truth: true, answer: "Oui."}
                        """)));
    }
}
