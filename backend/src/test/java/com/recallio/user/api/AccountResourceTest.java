package com.recallio.user.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.recallio.platform.domain.PersonalData;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** L'export et la suppression du compte (ADR 0056) : chaque module répond de ses données. */
@QuarkusTest
class AccountResourceTest {

    private static final String ME = "/api/v1/me";
    private static final String ACCOUNT = "/api/v1/me/account";
    private static final String EXPORT = ACCOUNT + "/export";

    /** Les tables dont les lignes de l'utilisateur s'effacent avec son compte. */
    private static final Set<String> ERASED = Set.of(
            "review_state",
            "review_log",
            "mock_exam",
            "tour_progress",
            "tour_last_page",
            "points_ledger",
            "daily_activity",
            "quest_progress",
            "quest_seen",
            "achievement",
            "push_subscription",
            "ranked_standing",
            "ranked_badge",
            "ranked_queue",
            "group_member",
            "followed_subject",
            "public_profile");

    /** Celles où elles restent, parce que d'autres les partagent : l'identifiant ne mène plus à personne. */
    private static final Set<String> KEPT =
            Set.of("duel_player", "duel_answer", "challenge_answer", "live_game_player", "content_report");

    @Inject
    EntityManager entityManager;

    @Test
    void shouldRequireAuthentication() {
        given().get(EXPORT).then().statusCode(401);
        given().delete(ACCOUNT).then().statusCode(401);
    }

    /**
     * Le garde-fou de l'ADR 0056 : une nouvelle table qui porte un utilisateur fait échouer ce test, jusqu'à ce que son
     * module l'exporte et dise ce qu'elle devient à la suppression du compte.
     */
    @Test
    void shouldKnowEveryTableThatHoldsAUser() {
        List<?> tables = QuarkusTransaction.requiringNew()
                .call(() -> entityManager.createNativeQuery("""
                        select table_name from information_schema.columns
                        where table_schema = current_schema() and column_name = 'user_id'
                        """, String.class).getResultList());

        Set<String> known = new TreeSet<>(ERASED);
        known.addAll(KEPT);
        assertThat(tables).map(Object::toString).containsExactlyInAnyOrderElementsOf(known);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(
            claims = {@Claim(key = "sub", value = "account-export"), @Claim(key = "name", value = "Alice Martin")})
    void shouldExportTheDataOfEveryModule() {
        UUID alice = profileId();
        seed(alice, "export");

        given().get(EXPORT)
                .then()
                .statusCode(200)
                .header("Content-Disposition", AccountResource.EXPORT_FILE)
                .body("exportedAt", notNullValue())
                .body(
                        "sections.keySet()",
                        containsInAnyOrder(
                                "feedback",
                                "gamification",
                                "group",
                                "live",
                                "notification",
                                "ranked",
                                "review",
                                "user"))
                .body("sections.user.app_user[0].display_name", equalTo("Alice Martin"))
                .body("sections.user.followed_subject", hasSize(1))
                .body("sections.review.review_state", hasSize(1))
                .body("sections.review.review_log[0].rating", equalTo(3))
                .body("sections.gamification.points_ledger[0].amount", equalTo(10))
                .body("sections.group.group_member", hasSize(2))
                .body("sections.group.challenge", hasSize(1))
                .body("sections.ranked.duel_player", hasSize(2))
                .body("sections.live.live_game_player[0].display_name", equalTo("Alice Martin"))
                .body("sections.feedback.content_report[0].comment", equalTo("Une coquille."))
                // Les clés de chiffrement d'un abonnement ne sortent pas du serveur.
                .body("sections.notification.push_subscription[0]", hasKey("endpoint"))
                .body("sections.notification.push_subscription[0]", not(hasKey("p256dh")))
                .body("sections.notification.push_subscription[0]", not(hasKey("auth")));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(
            claims = {@Claim(key = "sub", value = "account-delete"), @Claim(key = "name", value = "Alice Martin")})
    void shouldEraseTheAccountAndKeepWhatOthersShare() {
        UUID alice = profileId();
        Seeded seeded = seed(alice, "delete");

        given().delete(ACCOUNT).then().statusCode(204);

        for (String table : ERASED) {
            assertThat(count("select count(*) from " + table + " where user_id = :id", alice))
                    .as(table)
                    .isZero();
        }
        assertThat(count("select count(*) from app_user where id = :id", alice)).isZero();
        for (String table : KEPT) {
            assertThat(count("select count(*) from " + table + " where user_id = :id", alice))
                    .as(table)
                    .isPositive();
        }
        // Ce que les autres partagent avec elle reste, sans son nom.
        assertThat(text("select display_name from live_game_player where user_id = :id", alice))
                .isEqualTo(PersonalData.DELETED_ACCOUNT_NAME);
        assertThat(count("select count(*) from duel where id = :id", seeded.finishedDuel()))
                .isOne();
        assertThat(count("select count(*) from duel where id = :id", seeded.openDuel()))
                .isZero();
        // Le groupe où elle était seule part ; celui qu'elle partageait passe à son autre membre.
        assertThat(count("select count(*) from study_group where id = :id", seeded.ownGroup()))
                .isZero();
        assertThat(text("select role from group_member where user_id = :id", seeded.otherUser()))
                .isEqualTo("OWNER");
        // Les données de l'autre utilisateur ne bougent pas.
        assertThat(count("select count(*) from review_state where user_id = :id", seeded.otherUser()))
                .isOne();

        // Le compte du fournisseur d'identité existe encore : se reconnecter crée un profil neuf.
        assertThat(profileId()).isNotEqualTo(alice);
    }

    private static UUID profileId() {
        return UUID.fromString(given().get(ME).then().statusCode(200).extract().path("id"));
    }

    private record Seeded(UUID otherUser, UUID ownGroup, UUID finishedDuel, UUID openDuel) {}

    /**
     * Une ligne dans chaque table qui porte l'utilisateur, et un second utilisateur qui partage avec lui un groupe, un
     * duel terminé et une partie live. {@code tag} rend uniques les valeurs qui doivent l'être d'un test à l'autre.
     */
    private Seeded seed(UUID user, String tag) {
        UUID other = UUID.randomUUID();
        UUID ownGroup = UUID.randomUUID();
        UUID sharedGroup = UUID.randomUUID();
        UUID challenge = UUID.randomUUID();
        UUID finishedDuel = UUID.randomUUID();
        UUID openDuel = UUID.randomUUID();
        UUID game = UUID.randomUUID();
        UUID card = UUID.randomUUID();
        Map<String, Object> ids = Map.of(
                "user", user,
                "other", other,
                "ownGroup", ownGroup,
                "sharedGroup", sharedGroup,
                "challenge", challenge,
                "finishedDuel", finishedDuel,
                "openDuel", openDuel,
                "game", game,
                "card", card);
        List<String> statements = List.of(
                """
                insert into app_user (id, oidc_subject, display_name, timezone, created_at, updated_at)
                values (:other, 'other-of-%s', 'Bob Durand', 'UTC', now(), now())
                """.formatted(tag),
                "insert into followed_subject (user_id, subject_slug, followed_at) values (:user, 'java', now())",
                """
                insert into public_profile (user_id, handle, visible, updated_at)
                values (:user, 'alice-%s', true, now())
                """.formatted(tag),
                """
                insert into review_state (user_id, card_id, state, learning_step, stability, difficulty, due_at, reps,
                    lapses, last_review_at, introduced_at)
                select u, :card, 'REVIEW', 0, 1, 5, now(), 1, 0, now(), now() from unnest(array[:user, :other]) u
                """,
                """
                insert into review_log (id, request_id, user_id, card_id, card_version_id, rating, reviewed_at, state,
                    stability, difficulty, due_at)
                values (gen_random_uuid(), gen_random_uuid(), :user, :card, :card, 3, now(), 'REVIEW', 1, 5, now())
                """,
                """
                insert into mock_exam (id, user_id, subject_slug, started_at, deadline)
                values (gen_random_uuid(), :user, 'java', now(), now())
                """,
                "insert into tour_progress (user_id, notion_id, validated_at) values (:user, :card, now())",
                """
                insert into tour_last_page (user_id, subject_slug, notion_id, page, opened_at)
                values (:user, 'java', :card, 1, now())
                """,
                """
                insert into points_ledger (id, user_id, amount, reason, source_id, occurred_at)
                values (gen_random_uuid(), :user, 10, 'REVIEW', :card, now())
                """,
                """
                insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                values (:user, current_date, 1, false)
                """,
                """
                insert into quest_progress (user_id, quest, period_start, progress)
                values (:user, 'REVIEW_CARDS', current_date, 1)
                """,
                "insert into quest_seen (user_id, period_start, item) values (:user, current_date, 'x')",
                "insert into achievement (user_id, code, unlocked_at) values (:user, 'FIRST_REVIEW', now())",
                """
                insert into push_subscription (id, user_id, endpoint, p256dh, auth, reminder_hour, created_at)
                values (gen_random_uuid(), :user, 'https://push.example/%s', 'key', 'secret', 18, now())
                """.formatted(tag),
                """
                insert into content_report (id, user_id, subject_slug, card_id, reason, comment, created_at)
                values (gen_random_uuid(), :user, 'java', :card, 'TYPO', 'Une coquille.', now())
                """,
                """
                insert into ranked_standing (user_id, subject_slug, season, rating, deviation, volatility,
                    season_duels, wins, losses, draws, updated_at)
                values (:user, 'java', '2026-T4', 1500, 350, 0.06, 1, 1, 0, 0, now())
                """,
                """
                insert into ranked_badge (user_id, subject_slug, season, tier, awarded_at)
                values (:user, 'java', '2026-T3', 'GOLD', now())
                """,
                "insert into ranked_queue (user_id, subject_slug, heartbeat_at) values (:user, 'java', now())",
                """
                insert into duel (id, subject_slug, mode, status, season, created_at, expires_at, finished_at)
                values (:finishedDuel, 'java', 'ASYNC', 'FINISHED', '2026-T4', now(), now(), now()),
                       (:openDuel, 'java', 'ASYNC', 'OPEN', '2026-T4', now(), now(), null)
                """,
                """
                insert into duel_player (duel_id, user_id, seat, rating_before, deviation_before, volatility_before,
                    joined_at)
                values (:finishedDuel, :user, 0, 1500, 350, 0.06, now()),
                       (:finishedDuel, :other, 1, 1500, 350, 0.06, now()),
                       (:openDuel, :user, 0, 1500, 350, 0.06, now())
                """,
                """
                insert into duel_answer (duel_id, user_id, position, served_at, answered_at, correct)
                values (:finishedDuel, :user, 0, now(), now(), true)
                """,
                """
                insert into study_group (id, name, created_at, updated_at)
                values (:ownGroup, 'Seule', now(), now()), (:sharedGroup, 'À deux', now(), now())
                """,
                """
                insert into group_member (group_id, user_id, role, joined_at)
                values (:ownGroup, :user, 'OWNER', now()),
                       (:sharedGroup, :user, 'OWNER', now()),
                       (:sharedGroup, :other, 'MEMBER', now())
                """,
                """
                insert into challenge (id, group_id, title, seconds_per_card, created_by, created_at, ends_at)
                values (:challenge, :sharedGroup, 'Défi', 30, :user, now(), now() + interval '1 day')
                """,
                "insert into challenge_card (challenge_id, position, card_id) values (:challenge, 0, :card)",
                """
                insert into challenge_answer (challenge_id, user_id, position, presented_at, answered_at, correct)
                values (:challenge, :user, 0, now(), now(), true)
                """,
                """
                insert into live_game (id, group_id, subject_slug, host_id, started_at, finished_at, question_count)
                values (:game, :sharedGroup, 'java', :user, now(), now(), 3)
                """,
                """
                insert into live_game_player (game_id, user_id, display_name, rank, score, correct_count)
                values (:game, :user, 'Alice Martin', 1, 100, 3), (:game, :other, 'Bob Durand', 2, 50, 1)
                """);
        QuarkusTransaction.requiringNew().run(() -> {
            for (String statement : statements) {
                var query = entityManager.createNativeQuery(statement);
                ids.forEach((name, id) -> {
                    if (statement.contains(":" + name)) {
                        query.setParameter(name, id);
                    }
                });
                query.executeUpdate();
            }
        });
        return new Seeded(other, ownGroup, finishedDuel, openDuel);
    }

    private long count(String sql, UUID id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery(sql)
                                .setParameter("id", id)
                                .getSingleResult())
                        .longValue());
    }

    private String text(String sql, UUID id) {
        return QuarkusTransaction.requiringNew().call(() -> (String)
                entityManager.createNativeQuery(sql).setParameter("id", id).getSingleResult());
    }
}
