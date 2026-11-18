package com.recallio.gamification.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.CardIdentity;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.review.domain.Rating;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Le profil public de compétences (ADR 0043) : privé par défaut, le vérifié séparé du déclaré. */
@QuarkusTest
class PublicProfileTest {

    private static final String SETTINGS = "/api/v1/me/public-profile";
    private static final String PUBLIC = "/api/v1/public-profiles/";

    private static final String SUBJECT = """
            subject: haxe
            title: Haxe
            category: languages
            position: 970
            status: published
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: tout
                title: Tout
                notions:
                  - {id: bases, title: "Les bases", level: beginner, priority: 1}
            """;

    /**
     * Un vrai ou faux (la réponse juste est « Faux », le second choix), puis deux questions de cours à choix (la première
     * juste), ses sœurs : la file ne propose que la première carte le même jour (ADR 0031).
     */
    private static final String BASES = """
            notion: bases
            cards:
              - {key: vrai-faux, format: true-false, question: "Haxe ne compile que vers JavaScript.", truth: false, answer: "Non."}
              - key: cours
                format: course
                question: "Vers quoi Haxe compile-t-il ?"
                choices:
                  - {text: "Vers plusieurs langages", correct: true}
                  - {text: "Vers JavaScript seulement", correct: false}
                answer: "Vers JavaScript, C++, la JVM et d'autres."
              - key: cours-ancien
                format: course
                question: "Qui a créé Haxe ?"
                choices:
                  - {text: "Nicolas Cannasse", correct: true}
                  - {text: "Brendan Eich", correct: false}
                answer: "Nicolas Cannasse."
            """;

    @Inject
    ContentImportService contentImport;

    @Inject
    EntityManager entityManager;

    @BeforeEach
    void importSubject() {
        contentImport.importFolder(
                new SubjectFolder(SUBJECT, Map.of("01-bases.yaml", REFERENTIAL), Map.of("bases/bases.yaml", BASES)));
    }

    @Test
    void shouldReadAPublicProfileWithoutAnAccountButNotTheSettings() {
        given().get(PUBLIC + "personne-ici")
                .then()
                .statusCode(404)
                .body("code", is("gamification.public-profile-not-found"));
        given().get(PUBLIC + "Pas une adresse !").then().statusCode(404);
        given().get(SETTINGS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "public-profile-owner"))
    void shouldKeepTheProfilePrivateUntilTheUserMakesItPublic() {
        given().get(SETTINGS).then().statusCode(200).body("handle", nullValue()).body("visible", is(false));
        follow("haxe");
        answerRight();

        update("Alice-Dev", false)
                .statusCode(200)
                .body("handle", is("alice-dev"))
                .body("visible", is(false));
        given().get(PUBLIC + "alice-dev").then().statusCode(404);

        update("alice-dev", true).statusCode(200).body("visible", is(true));
        given().get(PUBLIC + "alice-dev")
                .then()
                .statusCode(200)
                .body("handle", is("alice-dev"))
                .body("displayName", notNullValue())
                .body("subjects.slug", contains("haxe"))
                .body("subjects[0].title", is("Haxe"))
                .body("subjects[0].verified.correctAnswers", is(1))
                .body("subjects[0].verified.exercisesPassed", is(0))
                .body("subjects[0].declared.notions", is(1))
                .body("achievements.achievement", hasItem("FIRST_REVIEW"));

        update("alice-dev", false).statusCode(200);
        given().get(PUBLIC + "alice-dev").then().statusCode(404);
    }

    /**
     * Le vérifié compte les réussites corrigées par le serveur, quel que soit le format (ADR 0063) : une question de cours
     * à choix en est une. Un ancien « Je savais », donné à une version de la carte qui n'avait pas encore ses choix, n'en
     * est pas une, même si la carte en a maintenant.
     */
    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "public-profile-self-assessed"))
    void shouldCountACourseQuestionWithChoicesButNotAnOldSelfAssessedSuccess() {
        follow("haxe");
        answerRight();
        given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"selectedChoices\": [0]}"
                        .formatted(UUID.randomUUID(), CardIdentity.keyedCardId("haxe", "bases", "cours")))
                .post("/api/v1/reviews")
                .then()
                .statusCode(201)
                .body("rating", is("GOOD"));
        recallAVersionWithoutChoices(CardIdentity.keyedCardId("haxe", "bases", "cours-ancien"));

        update("alice-declare", true).statusCode(200);
        given().get(PUBLIC + "alice-declare").then().statusCode(200).body("subjects[0].verified.correctAnswers", is(2));
    }

    /**
     * Un « Je savais » d'avant la fin de l'auto-évaluation, sur une version de la carte sans choix, écrit en base. La
     * version reste à l'écart de la version courante, et loin des numéros que donnerait un réimport.
     */
    private void recallAVersionWithoutChoices(UUID cardId) {
        QuarkusTransaction.requiringNew().run(() -> {
            UUID versionId = UUID.randomUUID();
            entityManager
                    .createNativeQuery("""
                            insert into card_version (id, card_id, version, prompt, answer, content_hash, created_at)
                            select :version, :card, max(version) + 1000, 'Qui a créé Haxe ?', 'Nicolas Cannasse.',
                                'sans-choix', now()
                            from card_version where card_id = :card
                            """)
                    .setParameter("version", versionId)
                    .setParameter("card", cardId)
                    .executeUpdate();
            entityManager
                    .createNativeQuery("""
                            insert into review_log (id, request_id, user_id, card_id, card_version_id, rating,
                                reviewed_at, state, stability, difficulty, due_at)
                            select gen_random_uuid(), gen_random_uuid(), u.id, :card, :version, :good,
                                now() - interval '30 days', 'REVIEW', 10, 5, now()
                            from app_user u where u.oidc_subject = 'public-profile-self-assessed'
                            """)
                    .setParameter("card", cardId)
                    .setParameter("version", versionId)
                    .setParameter("good", (short) Rating.GOOD.value())
                    .executeUpdate();
        });
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "public-profile-rules"))
    void shouldRefuseAMalformedOrTakenHandle() {
        update("-x", true).statusCode(422).body("code", is("user.invalid-handle"));
        update("a".repeat(31), true).statusCode(400);
        QuarkusTransaction.requiringNew().run(() -> {
            UUID other = UUID.randomUUID();
            entityManager
                    .createNativeQuery("""
                            insert into app_user (id, oidc_subject, display_name, timezone, created_at, updated_at)
                            values (:id, :sub, 'Bob', 'UTC', now(), now())
                            """)
                    .setParameter("id", other)
                    .setParameter("sub", "public-profile-other-" + other)
                    .executeUpdate();
            entityManager.createNativeQuery("""
                            insert into public_profile (user_id, handle, visible, updated_at)
                            values (:id, 'bob-prend-tout', false, now())
                            """).setParameter("id", other).executeUpdate();
        });

        update("bob-prend-tout", true).statusCode(409).body("code", is("user.handle-taken"));
    }

    private static ValidatableResponse update(String handle, boolean visible) {
        return given().contentType(ContentType.JSON)
                .body("{\"handle\": \"%s\", \"visible\": %s}".formatted(handle, visible))
                .put(SETTINGS)
                .then();
    }

    private static void follow(String subject) {
        given().contentType(ContentType.JSON)
                .body("{\"subjects\": [\"%s\"]}".formatted(subject))
                .put("/api/v1/me/followed-subjects")
                .then()
                .statusCode(200);
    }

    private static void answerRight() {
        String cardId = given().get("/api/v1/review-queue").then().extract().path("cards[0].cardId");
        given().contentType(ContentType.JSON)
                .body("{\"requestId\": \"%s\", \"cardId\": \"%s\", \"selectedChoices\": [1]}"
                        .formatted(UUID.randomUUID(), cardId))
                .post("/api/v1/reviews")
                .then()
                .statusCode(201)
                .body("rating", is("GOOD"));
    }
}
