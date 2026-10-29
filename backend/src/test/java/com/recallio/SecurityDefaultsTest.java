package com.recallio;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SecurityDefaultsTest {

    @Test
    void shouldRejectAnonymousRequestOnAnyApiPath() {
        given().when().get("/api/v1/anything").then().statusCode(401);
    }

    @Test
    void shouldAllowAnonymousHealthCheck() {
        given().when().get("/api/v1/health/ready").then().statusCode(200);
    }
}
