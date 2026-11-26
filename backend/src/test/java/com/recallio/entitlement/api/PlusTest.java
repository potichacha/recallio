package com.recallio.entitlement.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.entitlement.PlusAccess;
import com.recallio.entitlement.application.AccessCodeBatchService;
import com.recallio.entitlement.application.AccessCodeBatchService.CreatedBatch;
import com.recallio.entitlement.application.PlusService;
import com.recallio.entitlement.domain.AccessCode;
import com.recallio.entitlement.domain.CodeAttempts;
import com.recallio.entitlement.domain.EntitlementExceptions.AccessCodeAlreadyRedeemedException;
import com.recallio.entitlement.domain.EntitlementExceptions.InvalidAccessCodeException;
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
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Le droit « Plus » et le code d'accès qui l'ouvre (ADR 0079). Les lots sont créés par le service, comme le ferait un
 * admin : un test ne porte qu'une identité. L'horloge des tests avance : une date passée s'écrit en base.
 */
@QuarkusTest
class PlusTest {

    private static final String PLUS = "/api/v1/me/plus";
    private static final String CHECKS = PLUS + "/access-code-checks";
    private static final String REDEMPTIONS = PLUS + "/redemptions";

    private static final String INVALID = "entitlement.access-code-invalid";
    private static final String ALREADY_REDEEMED = "entitlement.access-code-already-redeemed";
    private static final String TOO_MANY_ATTEMPTS = "entitlement.too-many-code-attempts";

    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    private final SecureRandom random = new SecureRandom();

    @Inject
    AccessCodeBatchService batches;

    @Inject
    PlusService plus;

    @Inject
    PlusAccess plusAccess;

    @Inject
    Clock clock;

    @Inject
    EntityManager entityManager;

    @Test
    void shouldRequireAuthentication() {
        given().get(PLUS).then().statusCode(401);
        post(CHECKS, "K7QW2-M9XDF-4TLPA-8RBNC-H3VYE").statusCode(401);
        post(REDEMPTIONS, "K7QW2-M9XDF-4TLPA-8RBNC-H3VYE").statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-none"))
    void shouldHaveNoPlusBeforeAnyCode() {
        given().get(PLUS)
                .then()
                .statusCode(200)
                .body("active", is(false))
                .body("endsAt", nullValue())
                .body("daysLeft", is(0));
        assertThat(plusAccess.hasPlus(profileId())).isFalse();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-redeem"))
    void shouldOpenPlusWithACodeAndExtendItWithAnother() {
        UUID alice = profileId();
        String month = oneCode("Un mois", 30, 1, null);
        String week = oneCode("Une semaine", 7, 1, null);
        Instant before = clock.instant();

        // L'écran annonce ce que le code donnerait, sans l'utiliser.
        post(CHECKS, month).statusCode(200).body("plusDays", is(30));
        assertThat(redemptionsOf(month)).isZero();
        given().get(PLUS).then().statusCode(200).body("active", is(false));

        // Collé en minuscules, avec des espaces : la saisie est normalisée.
        String endsAt = post(REDEMPTIONS, "  " + month.toLowerCase().replace("-", " ") + " ")
                .statusCode(201)
                .body("plusDays", is(30))
                .body("plus.active", is(true))
                .body("plus.daysLeft", is(30))
                .extract()
                .path("plus.endsAt");
        Instant firstEnd = Instant.parse(endsAt);
        assertThat(firstEnd)
                .isBetween(
                        before.plus(Duration.ofDays(30)),
                        before.plus(Duration.ofDays(30)).plus(TOLERANCE));
        assertThat(redemptionsOf(month)).isOne();
        assertThat(plusAccess.hasPlus(alice)).isTrue();

        // Un second code prolonge le droit : ses jours s'ajoutent à la fin actuelle, pas à maintenant.
        post(REDEMPTIONS, week)
                .statusCode(201)
                .body("plusDays", is(7))
                .body("plus.daysLeft", is(37))
                .body("plus.endsAt", equalTo(firstEnd.plus(Duration.ofDays(7)).toString()));

        given().get(PLUS)
                .then()
                .statusCode(200)
                .body("active", is(true))
                .body("endsAt", equalTo(firstEnd.plus(Duration.ofDays(7)).toString()))
                .body("daysLeft", is(37));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-expiry"))
    void shouldEndPlusAtItsDateAndStartAgainFromNowWithANewCode() {
        UUID alice = profileId();
        post(REDEMPTIONS, oneCode("Un mois", 30, 1, null)).statusCode(201);
        Instant lapsed = clock.instant().minus(Duration.ofDays(3));

        update("update plus_entitlement set ends_at = :at where user_id = :id", lapsed, alice);

        given().get(PLUS)
                .then()
                .statusCode(200)
                .body("active", is(false))
                .body("endsAt", equalTo(lapsed.toString()))
                .body("daysLeft", is(0));
        assertThat(plusAccess.hasPlus(alice)).isFalse();

        // Les jours partent de maintenant : les trois jours sans Plus ne sont pas rattrapés.
        Instant before = clock.instant();
        String endsAt = post(REDEMPTIONS, oneCode("Une semaine", 7, 1, null))
                .statusCode(201)
                .body("plus.active", is(true))
                .body("plus.daysLeft", is(7))
                .extract()
                .path("plus.endsAt");
        assertThat(Instant.parse(endsAt))
                .isBetween(
                        before.plus(Duration.ofDays(7)),
                        before.plus(Duration.ofDays(7)).plus(TOLERANCE));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-twice"))
    void shouldRefuseTheSameCodeTwiceToTheSameAccount() {
        UUID alice = profileId();
        // Trois utilisations : le code n'est pas épuisé, c'est bien le compte qui l'a déjà utilisé.
        String code = oneCode("Partagé", 30, 3, null);
        String endsAt = post(REDEMPTIONS, code).statusCode(201).extract().path("plus.endsAt");

        post(REDEMPTIONS, code).statusCode(409).body("code", equalTo(ALREADY_REDEEMED));
        post(CHECKS, code).statusCode(409).body("code", equalTo(ALREADY_REDEEMED));

        assertThat(redemptionsOf(code)).isOne();
        given().get(PLUS).then().statusCode(200).body("endsAt", equalTo(endsAt));
        // Ce n'est pas un essai raté : le code existe, et ce compte le savait.
        assertThat(count("select count(*) from access_code_attempt where user_id = :id", alice))
                .isZero();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-invalid"))
    void shouldGiveTheSameErrorForAnUnknownExhaustedExpiredOrRevokedCode() {
        UUID alice = profileId();
        String unknown = AccessCode.generate(random).formatted();
        String exhausted = oneCode("Épuisé", 30, 1, null);
        plus.redeem(someoneElse(), exhausted);
        CreatedBatch expiring = batch("Expiré", 1, 30, 1, clock.instant().plus(Duration.ofDays(1)));
        update(
                "update access_code_batch set expires_at = :at where id = :id",
                clock.instant().minusSeconds(1),
                expiring.summary().batch().id());
        CreatedBatch revoked = batch("Révoqué", 1, 30, 1, null);
        batches.revoke(revoked.summary().batch().id());

        List<String> refusals = new ArrayList<>();
        for (String code : List.of(
                unknown,
                "pas-un-code",
                exhausted,
                expiring.codes().getFirst(),
                revoked.codes().getFirst())) {
            refusals.add(post(REDEMPTIONS, code)
                    .statusCode(422)
                    .contentType("application/problem+json")
                    .body("code", equalTo(INVALID))
                    .extract()
                    .asString());
            refusals.add(post(CHECKS, code).statusCode(422).extract().asString());
        }

        // Rien ne distingue un code qui n'a jamais existé d'un code qui ne marche plus.
        assertThat(refusals).hasSize(10).containsOnly(refusals.getFirst());
        assertThat(refusals.getFirst()).doesNotContain(unknown).contains("Ce code ne marche pas");
        given().get(PLUS).then().statusCode(200).body("active", is(false));
        assertThat(redemptionsOf(exhausted)).isOne();
        assertThat(count("select failures from access_code_attempt where user_id = :id", alice))
                .isEqualTo(10);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-validation"))
    void shouldRefuseARequestWithoutACode() {
        given().contentType(ContentType.JSON)
                .body(Map.of())
                .post(REDEMPTIONS)
                .then()
                .statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("invalidParams.name", contains("code"));
        post(CHECKS, " ").statusCode(400).body("code", equalTo("validation.failed"));
        post(REDEMPTIONS, "A".repeat(AccessCode.MAX_INPUT_LENGTH + 1))
                .statusCode(400)
                .body("code", equalTo("validation.failed"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-attempts"))
    void shouldBlockAnAccountAfterTenFailedAttemptsInTheHour() {
        UUID alice = profileId();
        String valid = oneCode("Valide", 30, 1, null);

        for (int attempt = 1; attempt <= CodeAttempts.MAX_FAILURES; attempt++) {
            String guess = AccessCode.generate(random).formatted();
            // Les deux routes comptent : sinon l'une servirait à deviner pour l'autre.
            post(attempt % 2 == 0 ? CHECKS : REDEMPTIONS, guess).statusCode(422).body("code", equalTo(INVALID));
        }

        // Même un code valide est refusé : sinon la limite n'arrêterait rien.
        post(REDEMPTIONS, valid)
                .statusCode(429)
                .contentType("application/problem+json")
                .body("code", equalTo(TOO_MANY_ATTEMPTS));
        post(CHECKS, valid).statusCode(429).body("code", equalTo(TOO_MANY_ATTEMPTS));
        assertThat(redemptionsOf(valid)).isZero();
        assertThat(count("select failures from access_code_attempt where user_id = :id", alice))
                .isEqualTo(CodeAttempts.MAX_FAILURES);

        // Une heure plus tard, d'après l'horloge : la fenêtre est finie.
        update(
                "update access_code_attempt set window_started_at = :at where user_id = :id",
                clock.instant().minus(CodeAttempts.WINDOW).minusSeconds(1),
                alice);

        post(REDEMPTIONS, AccessCode.generate(random).formatted()).statusCode(422);
        assertThat(count("select failures from access_code_attempt where user_id = :id", alice))
                .isOne();
        post(REDEMPTIONS, valid).statusCode(201).body("plus.active", is(true));
    }

    /** Dix comptes utilisent en même temps un code à trois utilisations : trois y arrivent, jamais quatre. */
    @Test
    void shouldNeverExceedTheUsesOfACodeUnderConcurrency() throws Exception {
        String code = oneCode("Trois places", 30, 3, null);

        List<Object> outcomes = concurrently(10, () -> plus.redeem(someoneElse(), code));

        assertThat(outcomes)
                .filteredOn(PlusService.Redemption.class::isInstance)
                .hasSize(3);
        assertThat(outcomes)
                .filteredOn(InvalidAccessCodeException.class::isInstance)
                .hasSize(7);
        assertThat(redemptionsOf(code)).isEqualTo(3);
        assertThat(count(
                        "select count(*) from access_code_redemption r join access_code c on c.id = r.code_id"
                                + " where c.code_hash = :id",
                        fingerprint(code)))
                .isEqualTo(3);
    }

    /** Le même compte envoie deux fois le même code en même temps : il n'est utilisé qu'une fois. */
    @Test
    void shouldRedeemACodeOnceForAnAccountThatSendsItTwiceAtOnce() throws Exception {
        String code = oneCode("Double clic", 30, 5, null);
        UserRef user = someoneElse();

        List<Object> outcomes = concurrently(6, () -> plus.redeem(user, code));

        assertThat(outcomes)
                .filteredOn(PlusService.Redemption.class::isInstance)
                .hasSize(1);
        assertThat(outcomes)
                .filteredOn(AccessCodeAlreadyRedeemedException.class::isInstance)
                .hasSize(5);
        assertThat(redemptionsOf(code)).isOne();
        assertThat(plus.status(user).daysLeft()).isEqualTo(30);
    }

    /** Deux codes différents, utilisés en même temps par le même compte : leurs jours se cumulent tous. */
    @Test
    void shouldAddUpTwoCodesRedeemedAtOnceByTheSameAccount() throws Exception {
        List<String> codes = batch("Cumul", 4, 10, 1, null).codes();
        UserRef user = someoneElse();
        List<String> queue = new ArrayList<>(codes);

        List<Object> outcomes = concurrently(4, () -> {
            String code;
            synchronized (queue) {
                code = queue.removeFirst();
            }
            return plus.redeem(user, code);
        });

        assertThat(outcomes).allMatch(PlusService.Redemption.class::isInstance);
        assertThat(plus.status(user).daysLeft()).isEqualTo(40);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "plus-account"))
    void shouldExportPlusAndEraseItWithTheAccountWithoutReopeningTheCode() {
        UUID alice = profileId();
        String code = oneCode("Usage unique", 30, 1, null);
        post(REDEMPTIONS, AccessCode.generate(random).formatted()).statusCode(422);
        post(REDEMPTIONS, code).statusCode(201);

        String export = given().get("/api/v1/me/account/export")
                .then()
                .statusCode(200)
                .body("sections.entitlement.plus_entitlement", hasSize(1))
                .body("sections.entitlement.plus_entitlement[0].ends_at", notNullValue())
                .body("sections.entitlement.access_code_redemption", hasSize(1))
                .body("sections.entitlement.access_code_redemption[0].plus_days", is(30))
                .body("sections.entitlement.access_code_redemption[0].redeemed_at", notNullValue())
                .body("sections.entitlement.access_code_redemption[0].code_suffix", equalTo(suffix(code)))
                .body("sections.entitlement.access_code_redemption[0]", not(hasKey("code_hash")))
                .body("sections.entitlement.access_code_attempt[0].failures", is(1))
                .extract()
                .asString();
        // Ni le code, ni son empreinte ne sortent du serveur.
        assertThat(export).doesNotContain(code, fingerprint(code));

        given().delete("/api/v1/me/account").then().statusCode(204);

        for (String table : List.of("plus_entitlement", "access_code_redemption", "access_code_attempt")) {
            assertThat(count("select count(*) from " + table + " where user_id = :id", alice))
                    .as(table)
                    .isZero();
        }
        // Le compteur du code reste : un compte recréé ne le réutilise pas.
        assertThat(redemptionsOf(code)).isOne();
        assertThat(profileId()).isNotEqualTo(alice);
        post(REDEMPTIONS, code).statusCode(422).body("code", equalTo(INVALID));
        given().get(PLUS).then().statusCode(200).body("active", is(false));
    }

    private String oneCode(String label, int plusDays, int maxRedemptions, @Nullable Instant expiresAt) {
        return batch(label, 1, plusDays, maxRedemptions, expiresAt).codes().getFirst();
    }

    private CreatedBatch batch(
            String label, int codeCount, int plusDays, int maxRedemptions, @Nullable Instant expiresAt) {
        return batches.create(label, codeCount, plusDays, maxRedemptions, expiresAt);
    }

    private static UserRef someoneElse() {
        return new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
    }

    private static ValidatableResponse post(String path, String code) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("code", code))
                .post(path)
                .then();
    }

    private static UUID profileId() {
        return UUID.fromString(
                given().get("/api/v1/me").then().statusCode(200).extract().path("id"));
    }

    private static String fingerprint(String code) {
        return AccessCode.parse(code).orElseThrow().fingerprint();
    }

    private static String suffix(String code) {
        return AccessCode.parse(code).orElseThrow().suffix();
    }

    /** Lance {@code task} sur autant de fils, tous lâchés en même temps ; rend ce que chacun a rendu, ou levé. */
    private static List<Object> concurrently(int threads, Callable<Object> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Object>> futures = new ArrayList<>();
            for (int thread = 0; thread < threads; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        return task.call();
                    } catch (RuntimeException refusal) {
                        return refusal;
                    }
                }));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get(1, TimeUnit.MINUTES));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private long redemptionsOf(String code) {
        return count("select redemptions from access_code where code_hash = :id", fingerprint(code));
    }

    private long count(String sql, Object id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery(sql)
                                .setParameter("id", id)
                                .getSingleResult())
                        .longValue());
    }

    private void update(String sql, Instant at, UUID id) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery(sql)
                        .setParameter("at", at)
                        .setParameter("id", id)
                        .executeUpdate());
    }
}
