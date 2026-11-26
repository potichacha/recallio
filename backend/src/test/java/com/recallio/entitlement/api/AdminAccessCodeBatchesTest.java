package com.recallio.entitlement.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.entitlement.application.PlusService;
import com.recallio.entitlement.domain.AccessCode;
import com.recallio.entitlement.domain.AccessCodeBatch;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Les lots de codes d'accès, pour un admin (ADR 0079). */
@QuarkusTest
class AdminAccessCodeBatchesTest {

    private static final String BATCHES = "/api/v1/admin/access-code-batches";
    private static final String CODE_FORMAT = "([2-9A-HJ-NP-Z]{5}-){4}[2-9A-HJ-NP-Z]{5}";

    @Inject
    PlusService plus;

    @Inject
    Clock clock;

    @Inject
    EntityManager entityManager;

    @Test
    void shouldRequireAuthentication() {
        create(newBatch("Sans jeton", 1, 30)).statusCode(401);
        given().get(BATCHES).then().statusCode(401);
        given().get(BATCHES + "/" + UUID.randomUUID()).then().statusCode(401);
        given().put(BATCHES + "/" + UUID.randomUUID() + "/revocation").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "codes-not-admin"))
    void shouldRefuseSomeoneWhoIsNotAnAdmin() {
        create(newBatch("Pas admin", 1, 30)).statusCode(403);
        given().get(BATCHES).then().statusCode(403);
        given().get(BATCHES + "/" + UUID.randomUUID()).then().statusCode(403);
        given().put(BATCHES + "/" + UUID.randomUUID() + "/revocation").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @OidcSecurity(claims = @Claim(key = "sub", value = "codes-create"))
    void shouldCreateABatchAndGiveItsCodesOnlyOnce() {
        Instant expiresAt = clock.instant().plus(Duration.ofDays(60));
        Map<String, Object> request = newBatch("Testeurs de la bêta", 3, 90);
        request.put("expiresAt", expiresAt.toString());

        JsonPath created = create(request)
                .statusCode(201)
                .body("batch.id", notNullValue())
                .body("batch.label", equalTo("Testeurs de la bêta"))
                .body("batch.plusDays", is(90))
                // Une utilisation par code, sauf mention.
                .body("batch.maxRedemptionsPerCode", is(1))
                .body("batch.codeCount", is(3))
                .body("batch.usedCodes", is(0))
                .body("batch.redemptions", is(0))
                .body("batch.expiresAt", equalTo(expiresAt.toString()))
                .body("batch.expired", is(false))
                .body("batch.revokedAt", nullValue())
                .body("codes", hasSize(3))
                .extract()
                .jsonPath();
        String batchId = created.getString("batch.id");
        List<String> codes = created.getList("codes", String.class);
        assertThat(codes).doesNotHaveDuplicates().allMatch(code -> code.matches(CODE_FORMAT));

        // La base ne garde que l'empreinte et les derniers caractères.
        List<String> stored =
                strings("select code_hash || ' ' || suffix from access_code where batch_id = :id order by id", batchId);
        assertThat(stored)
                .containsExactlyInAnyOrderElementsOf(codes.stream()
                        .map(code -> AccessCode.parse(code).orElseThrow())
                        .map(code -> code.fingerprint() + " " + code.suffix())
                        .toList());
        assertThat(String.join("\n", stored))
                .doesNotContain(codes)
                .doesNotContain(codes.getFirst().replace("-", ""));

        // Plus aucune route ne rend les codes : seulement leurs derniers caractères.
        String detail = given().get(BATCHES + "/" + batchId)
                .then()
                .statusCode(200)
                .body("batch.id", equalTo(batchId))
                .body("batch.codeCount", is(3))
                .body("codes", hasSize(3))
                .body("codes.redemptions", contains(0, 0, 0))
                .extract()
                .asString();
        assertThat(JsonPath.from(detail).getList("codes.suffix", String.class))
                .containsExactlyInAnyOrderElementsOf(codes.stream()
                        .map(code -> code.substring(code.length() - 4))
                        .toList());
        String list = given().get(BATCHES).then().statusCode(200).extract().asString();
        for (String code : codes) {
            assertThat(detail + list).doesNotContain(code, code.replace("-", ""));
        }
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @OidcSecurity(claims = @Claim(key = "sub", value = "codes-bounds"))
    void shouldRefuseABatchOutOfBounds() {
        invalid(newBatch(" ", 1, 30), "label");
        invalid(newBatch("x".repeat(AccessCodeBatch.MAX_LABEL_LENGTH + 1), 1, 30), "label");
        invalid(newBatch("Lot", 0, 30), "codeCount");
        invalid(newBatch("Lot", AccessCodeBatch.MAX_CODES + 1, 30), "codeCount");
        invalid(newBatch("Lot", 1, 0), "plusDays");
        invalid(newBatch("Lot", 1, AccessCodeBatch.MAX_PLUS_DAYS + 1), "plusDays");
        Map<String, Object> noUse = newBatch("Lot", 1, 30);
        noUse.put("maxRedemptionsPerCode", 0);
        invalid(noUse, "maxRedemptionsPerCode");

        Map<String, Object> expired = newBatch("Lot", 1, 30);
        expired.put("expiresAt", clock.instant().minusSeconds(60).toString());
        create(expired).statusCode(422).body("code", equalTo("entitlement.batch-already-expired"));

        given().get(BATCHES + "?size=" + (AdminAccessCodeBatchesResource.MAX_PAGE_SIZE + 1))
                .then()
                .statusCode(400)
                .body("invalidParams.name", contains("size"));
        given().get(BATCHES + "?page=-1").then().statusCode(400).body("invalidParams.name", contains("page"));
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @OidcSecurity(claims = @Claim(key = "sub", value = "codes-list"))
    void shouldListTheBatchesMostRecentFirstWithWhatTheirCodesBecame() {
        Map<String, Object> shared = newBatch("Liste : partagé", 2, 30);
        shared.put("maxRedemptionsPerCode", 5);
        JsonPath first = create(shared).statusCode(201).extract().jsonPath();
        String firstId = first.getString("batch.id");
        String second = create(newBatch("Liste : récent", 1, 7))
                .statusCode(201)
                .extract()
                .path("batch.id");
        // Deux comptes utilisent le premier code, personne le second.
        String code = first.getString("codes[0]");
        plus.redeem(someone(), code);
        plus.redeem(someone(), code);

        given().get(BATCHES + "?size=2")
                .then()
                .statusCode(200)
                .body("page", is(0))
                .body("size", is(2))
                .body("totalItems", greaterThanOrEqualTo(2))
                .body("items.id", contains(second, firstId))
                .body("items[1].label", equalTo("Liste : partagé"))
                .body("items[1].maxRedemptionsPerCode", is(5))
                .body("items[1].codeCount", is(2))
                .body("items[1].usedCodes", is(1))
                .body("items[1].redemptions", is(2))
                .body("items[0].usedCodes", is(0));
        given().get(BATCHES + "?size=1&page=1").then().statusCode(200).body("items.id", contains(firstId));
        given().get(BATCHES + "/" + firstId)
                .then()
                .statusCode(200)
                .body(
                        "codes.find { it.suffix == '%s' }.redemptions".formatted(code.substring(code.length() - 4)),
                        is(2));

        // Une date d'expiration passée, d'après l'horloge.
        update(
                "update access_code_batch set expires_at = :at where id = :id",
                clock.instant().minusSeconds(1),
                firstId);
        given().get(BATCHES + "/" + firstId).then().statusCode(200).body("batch.expired", is(true));
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @OidcSecurity(claims = @Claim(key = "sub", value = "codes-revoke"))
    void shouldRevokeABatchAndKeepThePlusItAlreadyOpened() {
        Map<String, Object> request = newBatch("À révoquer", 2, 30);
        JsonPath created = create(request).statusCode(201).extract().jsonPath();
        String batchId = created.getString("batch.id");
        UserRef early = someone();
        plus.redeem(early, created.getString("codes[0]"));

        String revokedAt = given().put(BATCHES + "/" + batchId + "/revocation")
                .then()
                .statusCode(200)
                .body("id", equalTo(batchId))
                .body("revokedAt", notNullValue())
                .body("redemptions", is(1))
                .extract()
                .path("revokedAt");
        // Révoquer deux fois ne change rien.
        given().put(BATCHES + "/" + batchId + "/revocation")
                .then()
                .statusCode(200)
                .body("revokedAt", equalTo(revokedAt));

        // Le code encore libre ne marche plus ; le droit déjà ouvert reste.
        given().contentType(ContentType.JSON)
                .body(Map.of("code", created.getString("codes[1]")))
                .post("/api/v1/me/plus/redemptions")
                .then()
                .statusCode(422)
                .body("code", equalTo("entitlement.access-code-invalid"));
        assertThat(plus.status(early).active()).isTrue();
        given().get(BATCHES + "?size=100")
                .then()
                .statusCode(200)
                .body("items.find { it.id == '%s' }.revokedAt".formatted(batchId), equalTo(revokedAt));

        given().put(BATCHES + "/" + UUID.randomUUID() + "/revocation")
                .then()
                .statusCode(404)
                .body("code", equalTo("entitlement.batch-not-found"));
        given().get(BATCHES + "/" + UUID.randomUUID()).then().statusCode(404);
    }

    private static Map<String, Object> newBatch(String label, int codeCount, int plusDays) {
        Map<String, Object> request = new HashMap<>();
        request.put("label", label);
        request.put("codeCount", codeCount);
        request.put("plusDays", plusDays);
        return request;
    }

    private static ValidatableResponse create(Map<String, Object> request) {
        return given().contentType(ContentType.JSON).body(request).post(BATCHES).then();
    }

    private static void invalid(Map<String, Object> request, String field) {
        create(request)
                .statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("invalidParams.name", contains(field));
    }

    private static UserRef someone() {
        return new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
    }

    private List<String> strings(String sql, String id) {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<?> rows = entityManager
                    .createNativeQuery(sql)
                    .setParameter("id", UUID.fromString(id))
                    .getResultList();
            return rows.stream().map(Object::toString).toList();
        });
    }

    private void update(String sql, Instant at, String id) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery(sql)
                        .setParameter("at", at)
                        .setParameter("id", UUID.fromString(id))
                        .executeUpdate());
    }
}
