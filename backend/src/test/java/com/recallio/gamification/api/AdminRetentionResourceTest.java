package com.recallio.gamification.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

/** La rétention (ADR 0050) : des chiffres agrégés, réservés aux admins. Le calcul est couvert par RetentionReportTest. */
@QuarkusTest
class AdminRetentionResourceTest {

    private static final String RETENTION = "/api/v1/admin/retention";

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void shouldMeasureTheRetentionAtOneSevenAndThirtyDaysForAnAdmin() {
        given().get(RETENTION)
                .then()
                .statusCode(200)
                .body("players", greaterThanOrEqualTo(0))
                .body("days.day", contains(1, 7, 30));
    }

    @Test
    @TestSecurity(user = "bob", roles = "user")
    void shouldBeReservedToAdmins() {
        given().get(RETENTION).then().statusCode(403);
    }

    @Test
    void shouldRequireAuthentication() {
        given().get(RETENTION).then().statusCode(401);
    }
}
