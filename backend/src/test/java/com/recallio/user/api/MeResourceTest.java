package com.recallio.user.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MeResourceTest {

    private static final String ME = "/api/v1/me";

    @Test
    void shouldRequireAuthentication() {
        given().get(ME).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(
            claims = {@Claim(key = "sub", value = "sub-first-login"), @Claim(key = "name", value = "Alice Martin")})
    void shouldCreateTheProfileOnFirstCallWithTheBrowserTimezone() {
        String id = given().header(MeResource.TIMEZONE_HEADER, "Europe/Paris")
                .get(ME)
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Alice Martin"))
                .body("timezone", equalTo("Europe/Paris"))
                .body("roles", empty())
                .extract()
                .path("id");

        given().header(MeResource.TIMEZONE_HEADER, "Asia/Tokyo")
                .get(ME)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("timezone", equalTo("Europe/Paris"));
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-admin"))
    void shouldExposeTheRolesOfTheUser() {
        given().get(ME).then().statusCode(200).body("roles", contains("admin"));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(
            claims = {@Claim(key = "sub", value = "sub-update"), @Claim(key = "preferred_username", value = "bob")})
    void shouldUpdateThePreferences() {
        given().get(ME).then().body("displayName", equalTo("bob"));

        given().contentType(ContentType.JSON)
                .body("{\"displayName\": \"Bob D.\", \"timezone\": \"America/Montreal\"}")
                .put(ME)
                .then()
                .statusCode(200)
                .body("displayName", equalTo("Bob D."))
                .body("timezone", equalTo("America/Montreal"));

        given().get(ME).then().body("displayName", equalTo("Bob D."));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-invalid"))
    void shouldRejectAnUnknownTimezoneWithAStableErrorCode() {
        given().contentType(ContentType.JSON)
                .body("{\"displayName\": \"Bob\", \"timezone\": \"Mars/Olympus\"}")
                .put(ME)
                .then()
                .statusCode(422)
                .contentType("application/problem+json")
                .body("code", equalTo("user.invalid-timezone"));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "sub-blank"))
    void shouldRejectABlankDisplayName() {
        given().contentType(ContentType.JSON)
                .body("{\"displayName\": \"  \", \"timezone\": \"UTC\"}")
                .put(ME)
                .then()
                .statusCode(400)
                .body("code", equalTo("validation.failed"))
                .body("invalidParams.name", hasItem("displayName"));
    }
}
