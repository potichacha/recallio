package com.recallio.group.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.recallio.group.application.StudyGroupService;
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
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Chaque test se connecte avec un {@code sub} différent. Pour ne dépendre que des groupes, un autre membre est
 * ajouté directement en base ({@link #addMember}).
 */
@QuarkusTest
class GroupsResourceTest {

    private static final String GROUPS = "/api/v1/groups";

    @Inject
    StudyGroupService groupService;

    @Inject
    EntityManager entityManager;

    /** L'horloge de l'application (midi UTC en test) : les dates écrites en base doivent la suivre, pas l'horloge réelle. */
    @Inject
    Clock clock;

    @Test
    void shouldRequireAuthentication() {
        given().get(GROUPS).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = {@Claim(key = "sub", value = "group-create"), @Claim(key = "name", value = "Alice")})
    void shouldCreateAGroupWhoseCreatorIsTheOwnerAndOnlyMember() {
        String id = create("  Équipe backend  ")
                .statusCode(201)
                .header("Location", containsString(GROUPS + "/"))
                .body("name", equalTo("Équipe backend"))
                .body("myRole", equalTo("OWNER"))
                .body("members", hasSize(1))
                .body("members[0].displayName", equalTo("Alice"))
                .body("members[0].me", is(true))
                .extract()
                .path("id");

        given().get(GROUPS)
                .then()
                .statusCode(200)
                .body("find { it.id == '%s' }.memberCount".formatted(id), is(1))
                .body("find { it.id == '%s' }.myRole".formatted(id), equalTo("OWNER"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "group-invalid"))
    void shouldRejectAnEmptyName() {
        create("   ").statusCode(400).body("code", equalTo("validation.failed"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "group-outsider"))
    void shouldHideTheGroupsOfOthers() {
        UUID othersGroup = groupOfSomeoneElse();

        given().get(GROUPS + "/" + othersGroup)
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("code", equalTo("group.not-found"));
        given().delete(GROUPS + "/" + othersGroup).then().statusCode(404);
        given().get(GROUPS).then().body("id", not(hasItem(othersGroup.toString())));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "group-member"))
    void shouldLetAMemberSeeTheGroupAndLeaveItButNotManageIt() {
        UUID groupId = groupOfSomeoneElse();
        UUID me = myId();
        addMember(groupId, me);

        given().get(GROUPS + "/" + groupId)
                .then()
                .statusCode(200)
                .body("myRole", equalTo("MEMBER"))
                .body("members.role", contains("OWNER", "MEMBER"))
                .body("members.find { it.me }.userId", equalTo(me.toString()));
        given().delete(GROUPS + "/" + groupId).then().statusCode(403).body("code", equalTo("group.owner-only"));

        given().delete(GROUPS + "/" + groupId + "/members/me").then().statusCode(204);
        given().get(GROUPS + "/" + groupId).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "group-owner"))
    void shouldLetTheOwnerRemoveAMemberAndDeleteTheGroupButNotLeaveIt() {
        String groupId = create("Mon groupe").extract().path("id");
        UUID member = UUID.randomUUID();
        addMember(UUID.fromString(groupId), member);

        given().delete(GROUPS + "/" + groupId + "/members/me")
                .then()
                .statusCode(409)
                .body("code", equalTo("group.owner-cannot-leave"));
        given().delete(GROUPS + "/" + groupId + "/members/" + myId()).then().statusCode(409);
        given().delete(GROUPS + "/" + groupId + "/members/" + UUID.randomUUID())
                .then()
                .statusCode(404)
                .body("code", equalTo("group.member-not-found"));

        given().delete(GROUPS + "/" + groupId + "/members/" + member).then().statusCode(204);
        given().get(GROUPS + "/" + groupId).then().body("members", hasSize(1));

        given().delete(GROUPS + "/" + groupId).then().statusCode(204);
        given().get(GROUPS + "/" + groupId).then().statusCode(404);
    }

    private static ValidatableResponse create(String name) {
        return given().contentType(ContentType.JSON)
                .body("{\"name\": \"%s\"}".formatted(name))
                .post(GROUPS)
                .then();
    }

    private static UUID myId() {
        return UUID.fromString(given().get("/api/v1/me").then().extract().path("id"));
    }

    private UUID groupOfSomeoneElse() {
        return groupService
                .create(new UserRef(UUID.randomUUID(), ZoneOffset.UTC), "Le groupe de quelqu'un d'autre")
                .id();
    }

    private void addMember(UUID groupId, UUID userId) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                                insert into group_member (group_id, user_id, role, joined_at)
                                values (:groupId, :userId, 'MEMBER', :now)
                                """)
                        .setParameter("groupId", groupId)
                        .setParameter("userId", userId)
                        .setParameter("now", clock.instant())
                        .executeUpdate());
    }
}
