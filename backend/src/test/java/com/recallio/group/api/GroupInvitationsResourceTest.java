package com.recallio.group.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.recallio.group.application.GroupInvitationService;
import com.recallio.group.application.StudyGroupService;
import com.recallio.group.domain.StudyGroup;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Le créateur d'un groupe ({@code sub} « invitation-owner ») gère son lien ; les autres tests reçoivent le lien du groupe
 * de quelqu'un d'autre, créé par les services.
 */
@QuarkusTest
class GroupInvitationsResourceTest {

    private static final String GROUPS = "/api/v1/groups";
    private static final String INVITATIONS = "/api/v1/invitations";

    @Inject
    StudyGroupService groupService;

    @Inject
    GroupInvitationService invitationService;

    @Inject
    EntityManager entityManager;

    @Test
    void shouldRequireAuthentication() {
        given().get(INVITATIONS + "/" + "a".repeat(32)).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "invitation-owner"))
    void shouldLetTheOwnerRenewAndRevokeTheLink() {
        String groupId = given().contentType("application/json")
                .body("{\"name\": \"Équipe invitée\"}")
                .post(GROUPS)
                .then()
                .extract()
                .path("id");
        String invitation = GROUPS + "/" + groupId + "/invitation";
        given().get(invitation).then().statusCode(404).body("code", equalTo("group.no-active-invitation"));

        String first = given().post(invitation).then().statusCode(201).extract().path("token");
        given().get(invitation).then().statusCode(200).body("token", equalTo(first));

        String second =
                given().post(invitation).then().statusCode(201).extract().path("token");
        given().get(invitation).then().body("token", not(equalTo(first)));
        given().get(INVITATIONS + "/" + first).then().statusCode(404);
        given().get(INVITATIONS + "/" + second).then().statusCode(200).body("alreadyMember", is(true));

        given().delete(invitation).then().statusCode(204);
        given().get(invitation).then().statusCode(404);
        given().get(INVITATIONS + "/" + second).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "invitation-guest"))
    void shouldLetTheGuestPreviewThenJoinTheGroupOnlyOnce() {
        Invited invited = groupOfSomeoneElseWithALink();
        String link = INVITATIONS + "/" + invited.token();
        given().get(GROUPS + "/" + invited.groupId()).then().statusCode(404);

        given().get(link)
                .then()
                .statusCode(200)
                .body("groupId", equalTo(invited.groupId().toString()))
                .body("groupName", equalTo("Le groupe de quelqu'un d'autre"))
                .body("memberCount", is(1))
                .body("alreadyMember", is(false));

        given().post(link + "/accept")
                .then()
                .statusCode(200)
                .body("myRole", equalTo("MEMBER"))
                .body("members", hasSize(2));
        given().post(link + "/accept").then().statusCode(200).body("members", hasSize(2));
        given().get(link).then().body("alreadyMember", is(true));

        given().get(GROUPS + "/" + invited.groupId() + "/invitation")
                .then()
                .statusCode(403)
                .body("code", equalTo("group.owner-only"));
        given().post(GROUPS + "/" + invited.groupId() + "/invitation").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "invitation-invalid"))
    void shouldRejectUnknownMalformedAndExpiredLinksAlike() {
        Invited invited = groupOfSomeoneElseWithALink();
        expire(invited.groupId());

        for (String token : new String[] {invited.token(), "b".repeat(32), "pas-un-jeton"}) {
            given().get(INVITATIONS + "/" + token)
                    .then()
                    .statusCode(404)
                    .contentType("application/problem+json")
                    .body("code", equalTo("group.invitation-not-found"));
            given().post(INVITATIONS + "/" + token + "/accept").then().statusCode(404);
        }
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "invitation-full"))
    void shouldRefuseToJoinAFullGroup() {
        Invited invited = groupOfSomeoneElseWithALink();
        fill(invited.groupId());

        given().post(INVITATIONS + "/" + invited.token() + "/accept")
                .then()
                .statusCode(409)
                .body("code", equalTo("group.full"));
    }

    private record Invited(UUID groupId, String token) {}

    private Invited groupOfSomeoneElseWithALink() {
        var owner = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        UUID groupId =
                groupService.create(owner, "Le groupe de quelqu'un d'autre").id();
        return new Invited(
                groupId, invitationService.renew(owner, groupId).token().value());
    }

    private void expire(UUID groupId) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("update group_invitation set expires_at = :past where group_id = :groupId")
                        .setParameter("past", Instant.now().minusSeconds(1))
                        .setParameter("groupId", groupId)
                        .executeUpdate());
    }

    /** Complète le groupe, dont le créateur est le premier membre. */
    private void fill(UUID groupId) {
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                                insert into group_member (group_id, user_id, role, joined_at)
                                select :groupId, gen_random_uuid(), 'MEMBER', now()
                                from generate_series(1, :count)
                                """)
                        .setParameter("groupId", groupId)
                        .setParameter("count", StudyGroup.MAX_MEMBERS - 1)
                        .executeUpdate());
    }
}
