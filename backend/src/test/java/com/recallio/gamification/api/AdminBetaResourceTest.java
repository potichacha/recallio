package com.recallio.gamification.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.content.application.ContentImportService;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.gamification.domain.beta.BetaReport;
import com.recallio.platform.domain.UsageFeature;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * La page « Bêta » des admins (ADR 0076) : chaque module compte dans ses tables, et rien ne désigne une personne. Les
 * tests Quarkus partagent leur base et s'exécutent l'un après l'autre : ce qui porte sur le sujet du test est exact, le
 * reste se compare avant et après les données semées. Le calcul des parts est couvert par BetaReportTest.
 */
@QuarkusTest
class AdminBetaResourceTest {

    private static final String BETA = "/api/v1/admin/beta";

    private static final String SUBJECT = "smalltalk";

    private static final String SUBJECT_HEADER = """
            subject: smalltalk
            title: Smalltalk
            category: languages
            position: 985
            status: published
            """;

    private static final String REFERENTIAL = """
            domain: bases
            title: Les bases
            position: 10
            chapters:
              - id: debut
                title: Débuter
                notions:
                  - {id: programme, title: "Le programme", level: beginner, priority: 1}
                  - {id: variables, title: "Les variables", level: beginner, priority: 1}
              - id: suite
                title: Poursuivre
                notions:
                  - {id: boucles, title: "Les boucles", level: beginner, priority: 1}
            """;

    private static final String PAGE = """
            tour:
              - text: Une page.
                example:
                  files:
                    main.go: |-
                      package main

                      func main() {}
                  output: rien
            """;

    private static final String PROGRAMME = "notion: programme\n" + PAGE + """
            cards:
              - {key: ratee, format: true-false, question: "Un programme Smalltalk finit par une virgule.", truth: false, answer: "Non."}
              - {key: reussie, format: true-false, question: "Un programme Smalltalk commence par program.", truth: true, answer: "Oui."}
              - {key: rare, format: true-false, question: "Smalltalk date de 1870.", truth: false, answer: "Non."}
            """;

    private static final String VARIABLES = "notion: variables\n" + PAGE + """
            cards:
              - {key: var, format: true-false, question: "Une variable Smalltalk se déclare avec var.", truth: true, answer: "Oui."}
            """;

    private static final String BOUCLES = "notion: boucles\n" + PAGE + """
            cards:
              - {key: for, format: true-false, question: "Smalltalk a une boucle for.", truth: true, answer: "Oui."}
            """;

    private static final String FAILED_QUESTION = "Un programme Smalltalk finit par une virgule.";
    private static final String DISPLAY_NAME = "Testeuse de la bêta";

    private static final int AGAIN = 1;
    private static final int GOOD = 3;

    @Inject
    ContentImportService contentImport;

    @Inject
    CardDirectory cards;

    @Inject
    EntityManager entityManager;

    @Inject
    Clock clock;

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void shouldBeReservedToAdmins() {
        given().get(BETA).then().statusCode(403);
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(BETA).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void shouldReportWhatTheTestersDoWithoutNamingAnyone() {
        importSubject();
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant longAgo = now.minus(Duration.ofDays(BetaReport.DAYS + 10));
        Map<String, UUID> cardIds = cardIdsByQuestion();
        UUID failed = Objects.requireNonNull(cardIds.get(FAILED_QUESTION));
        UUID recalled = Objects.requireNonNull(cardIds.get("Un programme Smalltalk commence par program."));
        UUID rare = Objects.requireNonNull(cardIds.get("Smalltalk date de 1870."));
        UUID programme = notionId("programme");
        UUID variables = notionId("variables");
        UUID boucles = notionId("boucles");
        // Alice utilise tout, Bob révise, Erin ne passe qu'un examen blanc ; Dave n'a rien fait sur la fenêtre.
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID dave = UUID.randomUUID();
        UUID erin = UUID.randomUUID();
        JsonPath before = report();

        QuarkusTransaction.requiringNew().run(() -> {
            account(alice, now);
            account(bob, now);
            account(dave, longAgo);
            follow(alice, now);
            follow(bob, now);
            // Six réponses à la carte ratée, dont cinq fausses ; une ancienne, hors de la fenêtre.
            review(alice, failed, AGAIN, now, 4);
            review(bob, failed, AGAIN, now, 1);
            review(bob, failed, GOOD, now, 1);
            review(bob, failed, AGAIN, longAgo, 1);
            review(alice, recalled, GOOD, now, 5);
            // Quatre réponses fausses : sous le minimum, la carte n'entre pas dans les plus ratées.
            review(alice, rare, AGAIN, now, 4);
            signal(alice, failed, now, "null, null");
            signal(bob, failed, now, "null, null");
            signal(dave, failed, now, "'FIXED', :at");
            activity(alice, today, 13, true);
            activity(bob, today, 2, false);
            validate(alice, programme, now);
            validate(alice, variables, now);
            validate(alice, boucles, longAgo);
            lastPage(alice, programme, now);
            lastPage(bob, variables, now);
            lastPage(dave, variables, longAgo);
            mockExam(alice, now);
            mockExam(erin, now);
            everyOtherFeature(alice, failed, programme, now, today);
            reminders(dave, now);
            group(dave, now);
        });
        JsonPath after = report();

        assertThat(after.getString("from"))
                .isEqualTo(today.minusDays(BetaReport.DAYS - 1).toString());
        assertThat(after.getString("to")).isEqualTo(today.toString());
        assertThat(after.getList("days")).hasSize(BetaReport.DAYS);
        assertThat(after.getString("days[-1].day")).isEqualTo(today.toString());
        assertThat(todayDelta(before, after, "reviewers")).isEqualTo(2);
        assertThat(todayDelta(before, after, "reviews")).isEqualTo(15);
        assertThat(todayDelta(before, after, "goalsReached")).isEqualTo(1);
        assertThat(todayDelta(before, after, "accountsCreated")).isEqualTo(2);
        assertThat(todayDelta(before, after, "tourValidations")).isEqualTo(2);

        String subject = "subjects.find { it.subject == '%s' }.".formatted(SUBJECT);
        assertThat(after.getString(subject + "title")).isEqualTo("Smalltalk");
        assertThat(after.getLong(subject + "followers")).isEqualTo(2);
        assertThat(after.getLong(subject + "reviewers")).isEqualTo(2);
        assertThat(after.getLong(subject + "reviews")).isEqualTo(15);
        assertThat(after.getInt(subject + "successPercent")).isEqualTo(40);

        String failedCard = "failedCards.find { it.card.cardId == '%s' }.".formatted(failed);
        assertThat(after.getString(failedCard + "card.subject")).isEqualTo(SUBJECT);
        assertThat(after.getString(failedCard + "card.subjectTitle")).isEqualTo("Smalltalk");
        assertThat(after.getString(failedCard + "card.notionTitle")).isEqualTo("Le programme");
        assertThat(after.getString(failedCard + "card.excerpt")).isEqualTo(FAILED_QUESTION);
        assertThat(after.getString(failedCard + "card.type")).isEqualTo("TRUE_FALSE");
        assertThat(after.getLong(failedCard + "answers")).isEqualTo(6);
        assertThat(after.getLong(failedCard + "failures")).isEqualTo(5);
        assertThat(after.getInt(failedCard + "failurePercent")).isEqualTo(83);
        assertThat(after.getLong(failedCard + "openReports")).isEqualTo(2);
        assertThat(after.getList("failedCards.card.cardId", String.class))
                .hasSizeLessThanOrEqualTo(BetaReport.FAILED_CARDS)
                .doesNotContain(recalled.toString(), rare.toString());
        assertThat(after.getLong("reportedCards.find { it.card.cardId == '%s' }.openReports".formatted(failed)))
                .isEqualTo(2);

        String stops = "tour.stops.find { it.subject == '%s' }.".formatted(SUBJECT);
        assertThat(after.getList(stops + "chapters.chapterTitle")).containsExactly("Débuter", "Poursuivre");
        assertThat(after.getList(stops + "chapters.domainTitle")).containsOnly("Les bases");
        // Alice et Bob dans « Débuter », l'un sur « programme », l'autre sur « variables », avec Dave : seule la
        // dernière page compte, quelle que soit sa date.
        assertThat(after.getList(stops + "chapters.accounts")).containsExactly(3, 0);
        assertThat(after.getInt("tour.learners") - before.getInt("tour.learners"))
                .isEqualTo(1);

        assertThat(after.getInt("features.activeAccounts") - before.getInt("features.activeAccounts"))
                .isEqualTo(3);
        assertThat(after.getList("features.features.feature")).containsExactly(featureNames());
        for (UsageFeature feature : UsageFeature.values()) {
            int expected = switch (feature) {
                case REVIEW, MOCK_EXAM -> 2;
                // Aucun exercice dans le sujet du test.
                case CODING_EXERCISE -> 0;
                // Dave a un rappel et un groupe, mais n'est pas actif : seule Alice compte.
                default -> 1;
            };
            assertThat(accounts(after, feature) - accounts(before, feature))
                    .as(feature.name())
                    .isEqualTo(expected);
            assertThat(after.getBoolean(feature(feature) + "dated")).isEqualTo(feature.isDated());
        }
        assertThat(after.getInt(feature(UsageFeature.REVIEW) + "percent"))
                .isEqualTo(accounts(after, UsageFeature.REVIEW) * 100 / after.getInt("features.activeAccounts"));

        String json = after.prettify();
        assertThat(json)
                .doesNotContain(alice.toString(), bob.toString(), dave.toString(), erin.toString())
                .doesNotContain(DISPLAY_NAME)
                .doesNotContainIgnoringCase("userId")
                .doesNotContainIgnoringCase("user_id")
                .doesNotContainIgnoringCase("displayName");
        // Les tests des signalements lisent la file des admins : elle ne garde rien de celui-ci.
        QuarkusTransaction.requiringNew()
                .run(() -> sql("delete from content_report where subject_slug = :subject")
                        .setParameter("subject", SUBJECT)
                        .executeUpdate());
    }

    private static JsonPath report() {
        return given().get(BETA).then().statusCode(200).extract().jsonPath();
    }

    private static long todayDelta(JsonPath before, JsonPath after, String field) {
        return after.getLong("days[-1]." + field) - before.getLong("days[-1]." + field);
    }

    private static String feature(UsageFeature feature) {
        return "features.features.find { it.feature == '%s' }.".formatted(feature.name());
    }

    private static int accounts(JsonPath report, UsageFeature feature) {
        return report.getInt(feature(feature) + "accounts");
    }

    private static Object[] featureNames() {
        return Arrays.stream(UsageFeature.values()).map(Enum::name).toArray();
    }

    private void importSubject() {
        contentImport.importFolder(new SubjectFolder(
                SUBJECT_HEADER,
                Map.of("01-bases.yaml", REFERENTIAL),
                Map.of(
                        "bases/programme.yaml", PROGRAMME,
                        "bases/variables.yaml", VARIABLES,
                        "bases/boucles.yaml", BOUCLES)));
    }

    private UUID notionId(String slug) {
        return cards.notion(SUBJECT, slug).map(NotionInfo::id).orElseThrow();
    }

    private Map<String, UUID> cardIdsByQuestion() {
        List<UUID> cardIds = cards.activeCards(new CardFilter(Set.of(SUBJECT), null, Set.of(), Set.of())).stream()
                .map(CardDirectory.ActiveCard::cardId)
                .toList();
        return cards.currentVersions(cardIds).stream()
                .collect(Collectors.toMap(CardSnapshot::prompt, CardSnapshot::cardId));
    }

    private void account(UUID userId, Instant createdAt) {
        sql("""
                insert into app_user (id, oidc_subject, display_name, timezone, created_at, updated_at)
                values (:id, :subject, :name, 'UTC', :at, :at)
                """)
                .setParameter("id", userId)
                .setParameter("subject", "beta-" + userId)
                .setParameter("name", DISPLAY_NAME)
                .setParameter("at", createdAt)
                .executeUpdate();
    }

    private void follow(UUID userId, Instant at) {
        sql("insert into followed_subject (user_id, subject_slug, followed_at) values (:userId, :subject, :at)")
                .setParameter("userId", userId)
                .setParameter("subject", SUBJECT)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void review(UUID userId, UUID cardId, int rating, Instant at, int times) {
        for (int i = 0; i < times; i++) {
            sql("""
                    insert into review_log (id, request_id, user_id, card_id, card_version_id, rating, reviewed_at,
                                            state, stability, difficulty, due_at)
                    values (:id, :requestId, :userId, :cardId, :versionId, :rating, :at, 'REVIEW', 1, 5, :at)
                    """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("requestId", UUID.randomUUID())
                    .setParameter("userId", userId)
                    .setParameter("cardId", cardId)
                    .setParameter("versionId", UUID.randomUUID())
                    .setParameter("rating", rating)
                    .setParameter("at", at)
                    .executeUpdate();
        }
    }

    /** @param resolution la résolution et sa date, telles qu'en SQL : un signalement ouvert n'en a pas */
    private void signal(UUID userId, UUID cardId, Instant at, String resolution) {
        sql("""
                insert into content_report (id, user_id, subject_slug, card_id, reason, comment, created_at,
                                            resolution, resolved_at)
                values (:id, :userId, :subject, :cardId, 'TYPO', '', :at, %s)
                """.formatted(resolution))
                .setParameter("id", UUID.randomUUID())
                .setParameter("userId", userId)
                .setParameter("subject", SUBJECT)
                .setParameter("cardId", cardId)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void activity(UUID userId, LocalDate day, int cardsReviewed, boolean goalReached) {
        sql("""
                insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                values (:userId, :day, :cards, :goal)
                """)
                .setParameter("userId", userId)
                .setParameter("day", day)
                .setParameter("cards", cardsReviewed)
                .setParameter("goal", goalReached)
                .executeUpdate();
    }

    private void validate(UUID userId, UUID notionId, Instant at) {
        sql("insert into tour_progress (user_id, notion_id, validated_at) values (:userId, :notionId, :at)")
                .setParameter("userId", userId)
                .setParameter("notionId", notionId)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void lastPage(UUID userId, UUID notionId, Instant at) {
        sql("""
                insert into tour_last_page (user_id, subject_slug, notion_id, page, opened_at)
                values (:userId, :subject, :notionId, 1, :at)
                """)
                .setParameter("userId", userId)
                .setParameter("subject", SUBJECT)
                .setParameter("notionId", notionId)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void mockExam(UUID userId, Instant at) {
        sql("""
                insert into mock_exam (id, user_id, subject_slug, started_at, deadline)
                values (:id, :userId, :subject, :at, :at)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("userId", userId)
                .setParameter("subject", SUBJECT)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void reminders(UUID userId, Instant at) {
        sql("""
                insert into push_subscription (id, user_id, endpoint, p256dh, auth, reminder_hour, created_at)
                values (:id, :userId, :endpoint, 'cle', 'secret', 18, :at)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("userId", userId)
                .setParameter("endpoint", "https://push.example/" + userId)
                .setParameter("at", at)
                .executeUpdate();
    }

    /** @return le groupe créé, dont l'utilisateur est le créateur */
    private UUID group(UUID userId, Instant at) {
        UUID groupId = UUID.randomUUID();
        sql("insert into study_group (id, name, created_at, updated_at) values (:id, 'Bêta', :at, :at)")
                .setParameter("id", groupId)
                .setParameter("at", at)
                .executeUpdate();
        sql("insert into group_member (group_id, user_id, role, joined_at) values (:id, :userId, 'OWNER', :at)")
                .setParameter("id", groupId)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .executeUpdate();
        return groupId;
    }

    /** Une ligne dans la table de chacune des fonctionnalités que les autres méthodes ne sèment pas. */
    private void everyOtherFeature(UUID userId, UUID cardId, UUID notionId, Instant at, LocalDate today) {
        reminders(userId, at);
        UUID groupId = group(userId, at);
        groupChallenge(userId, groupId, cardId, at);
        dailyChallenge(userId, cardId, at, today);
        duel(userId, at);
        liveGame(userId, groupId, at);
        sql("""
                insert into quest_progress (user_id, quest, period_start, progress, completed_at)
                values (:userId, 'BETA', :day, 1, :at)
                """)
                .setParameter("userId", userId)
                .setParameter("day", today)
                .setParameter("at", at)
                .executeUpdate();
        sql("""
                insert into drill_activity (user_id, local_date, answers, correct)
                values (:userId, :day, 3, 2)
                """).setParameter("userId", userId).setParameter("day", today).executeUpdate();
        sql("""
                insert into known_notion (user_id, notion_id, subject_slug, known_at)
                values (:userId, :notionId, :subject, :at)
                """)
                .setParameter("userId", userId)
                .setParameter("notionId", notionId)
                .setParameter("subject", SUBJECT)
                .setParameter("at", at)
                .executeUpdate();
        sql("""
                insert into interview_plan (user_id, subject_slug, interview_on, planned_on, notions_to_acquire)
                values (:userId, :subject, :interview, :planned, 3)
                """)
                .setParameter("userId", userId)
                .setParameter("subject", SUBJECT)
                .setParameter("interview", today.plusDays(20))
                .setParameter("planned", today)
                .executeUpdate();
        sql("insert into public_profile (user_id, handle, visible, updated_at) values (:userId, :handle, true, :at)")
                .setParameter("userId", userId)
                .setParameter("handle", "beta-" + userId.toString().substring(0, 8))
                .setParameter("at", at)
                .executeUpdate();
        plus(userId, at);
    }

    /** Le droit « Plus » en cours, ouvert par un code d'accès (ADR 0079). */
    private void plus(UUID userId, Instant at) {
        UUID batchId = UUID.randomUUID();
        UUID codeId = UUID.randomUUID();
        sql("insert into plus_entitlement (user_id, ends_at, updated_at) values (:userId, :endsAt, :at)")
                .setParameter("userId", userId)
                .setParameter("endsAt", at.plus(Duration.ofDays(30)))
                .setParameter("at", at)
                .executeUpdate();
        sql("""
                insert into access_code_batch (id, label, plus_days, max_redemptions_per_code, created_at)
                values (:id, 'Bêta', 30, 1, :at)
                """).setParameter("id", batchId).setParameter("at", at).executeUpdate();
        sql("""
                insert into access_code (id, batch_id, code_hash, suffix, redemptions)
                values (:id, :batchId, :hash, 'BETA', 1)
                """)
                .setParameter("id", codeId)
                .setParameter("batchId", batchId)
                .setParameter("hash", "beta-" + codeId)
                .executeUpdate();
        sql("""
                insert into access_code_redemption (code_id, user_id, redeemed_at, plus_days)
                values (:id, :userId, :at, 30)
                """)
                .setParameter("id", codeId)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void groupChallenge(UUID userId, UUID groupId, UUID cardId, Instant at) {
        UUID challengeId = UUID.randomUUID();
        sql("""
                insert into challenge (id, group_id, title, seconds_per_card, created_by, created_at, ends_at)
                values (:id, :groupId, 'Défi', 30, :userId, :at, :end)
                """)
                .setParameter("id", challengeId)
                .setParameter("groupId", groupId)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .setParameter("end", at.plus(Duration.ofDays(1)))
                .executeUpdate();
        sql("insert into challenge_card (challenge_id, position, card_id) values (:id, 0, :cardId)")
                .setParameter("id", challengeId)
                .setParameter("cardId", cardId)
                .executeUpdate();
        sql("""
                insert into challenge_answer (challenge_id, user_id, position, presented_at)
                values (:id, :userId, 0, :at)
                """)
                .setParameter("id", challengeId)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void dailyChallenge(UUID userId, UUID cardId, Instant at, LocalDate today) {
        UUID challengeId = UUID.randomUUID();
        sql("insert into daily_challenge (id, subject_slug, day, created_at) values (:id, :subject, :day, :at)")
                .setParameter("id", challengeId)
                .setParameter("subject", SUBJECT)
                .setParameter("day", today)
                .setParameter("at", at)
                .executeUpdate();
        sql("insert into daily_challenge_question (challenge_id, position, card_id) values (:id, 0, :cardId)")
                .setParameter("id", challengeId)
                .setParameter("cardId", cardId)
                .executeUpdate();
        sql("""
                insert into daily_challenge_answer (challenge_id, user_id, position, correct, answered_at, request_id)
                values (:id, :userId, 0, true, :at, :requestId)
                """)
                .setParameter("id", challengeId)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .setParameter("requestId", UUID.randomUUID())
                .executeUpdate();
    }

    private void duel(UUID userId, Instant at) {
        UUID duelId = UUID.randomUUID();
        sql("""
                insert into duel (id, subject_slug, mode, status, season, created_at, expires_at)
                values (:id, :subject, 'ASYNC', 'EXPIRED', '2026-T4', :at, :at)
                """)
                .setParameter("id", duelId)
                .setParameter("subject", SUBJECT)
                .setParameter("at", at)
                .executeUpdate();
        sql("""
                insert into duel_player (duel_id, user_id, seat, rating_before, deviation_before, volatility_before,
                                         joined_at)
                values (:id, :userId, 0, 1500, 350, 0.06, :at)
                """)
                .setParameter("id", duelId)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .executeUpdate();
    }

    private void liveGame(UUID userId, UUID groupId, Instant at) {
        UUID gameId = UUID.randomUUID();
        sql("""
                insert into live_game (id, group_id, subject_slug, host_id, started_at, finished_at, question_count)
                values (:id, :groupId, :subject, :userId, :at, :at, 5)
                """)
                .setParameter("id", gameId)
                .setParameter("groupId", groupId)
                .setParameter("subject", SUBJECT)
                .setParameter("userId", userId)
                .setParameter("at", at)
                .executeUpdate();
        sql("""
                insert into live_game_player (game_id, user_id, display_name, rank, score, correct_count)
                values (:id, :userId, :name, 1, 100, 5)
                """)
                .setParameter("id", gameId)
                .setParameter("userId", userId)
                .setParameter("name", DISPLAY_NAME)
                .executeUpdate();
    }

    private Query sql(String statement) {
        return entityManager.createNativeQuery(statement);
    }
}
