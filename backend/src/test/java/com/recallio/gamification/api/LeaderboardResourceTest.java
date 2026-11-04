package com.recallio.gamification.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.gamification.domain.leaderboard.Week;
import com.recallio.group.LeaderboardVisibility;
import com.recallio.group.application.GroupSettingsService;
import com.recallio.group.application.StudyGroupService;
import com.recallio.group.domain.GroupSettings;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Un groupe à trois : son créateur (régulier), l'utilisateur connecté (beaucoup d'XP en un jour) et un membre
 * inactif. L'activité est écrite directement en base, dans la semaine en cours du fuseau de l'utilisateur.
 */
@QuarkusTest
class LeaderboardResourceTest {

    private static final int GOAL = GroupSettings.DEFAULT_DAILY_GOAL;

    @Inject
    StudyGroupService groupService;

    @Inject
    GroupSettingsService settingsService;

    @Inject
    EntityManager entityManager;

    /** L'horloge de l'application (midi UTC en test) : les dates écrites en base doivent la suivre, pas l'horloge réelle. */
    @Inject
    Clock clock;

    @Test
    void shouldRequireAuthentication() {
        given().get(leaderboard(UUID.randomUUID())).then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = {@Claim(key = "sub", value = "leaderboard-named"), @Claim(key = "name", value = "Bob")})
    void shouldRankRegularityBeforeXp() {
        Group group = groupOfThree();

        given().get(leaderboard(group.id()))
                .then()
                .statusCode(200)
                .body("visibility", equalTo("NAMED"))
                .body("dailyGoal", is(GOAL))
                .body("weekStart", equalTo(group.week().monday().toString()))
                .body("entries.rank", contains(1, 2, 3))
                .body("entries.regularDays", contains(3, 1, 0))
                .body("entries.xp", contains(30, 500, 0))
                .body("entries.cardsReviewed", contains(3 * GOAL, 2 * GOAL, 0))
                .body("entries.me", contains(false, true, false))
                .body("entries[1].displayName", equalTo("Bob"))
                .body("entries[0].displayName", equalTo("Membre"));
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = {@Claim(key = "sub", value = "leaderboard-anonymous"), @Claim(key = "name", value = "Bob")})
    void shouldHideTheNamesOfOthersWhenAnonymous() {
        Group group = groupOfThree();
        configure(group, LeaderboardVisibility.ANONYMOUS);

        given().get(leaderboard(group.id()))
                .then()
                .body("visibility", equalTo("ANONYMOUS"))
                .body("entries[0].displayName", nullValue())
                .body("entries[1].displayName", equalTo("Bob"))
                .body("entries[2].displayName", nullValue());
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "leaderboard-hidden"))
    void shouldShowNothingWhenTheOwnerDisabledIt() {
        Group group = groupOfThree();
        configure(group, LeaderboardVisibility.HIDDEN);

        given().get(leaderboard(group.id()))
                .then()
                .statusCode(200)
                .body("visibility", equalTo("HIDDEN"))
                .body("entries", empty());
    }

    @Test
    @TestSecurity(user = "bob")
    @OidcSecurity(claims = @Claim(key = "sub", value = "leaderboard-outsider"))
    void shouldHideTheLeaderboardOfAGroupFromOutsiders() {
        UUID groupId = groupService
                .create(new UserRef(UUID.randomUUID(), ZoneOffset.UTC), "Pas le mien")
                .id();

        given().get(leaderboard(groupId)).then().statusCode(404).body("code", equalTo("group.not-found"));
    }

    private record Group(UUID id, UserRef owner, Week week) {}

    /** Le créateur atteint l'objectif 3 jours ; l'utilisateur, 1 jour avec beaucoup d'XP ; le troisième, rien. */
    private Group groupOfThree() {
        var owner = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        UUID groupId = groupService.create(owner, "Classement").id();
        UUID me = myId();
        addMember(groupId, me);
        addMember(groupId, UUID.randomUUID());

        Week week = Week.containing(LocalDate.now(myTimezone()));
        QuarkusTransaction.requiringNew().run(() -> {
            for (int day = 0; day < 3; day++) {
                activity(owner.id(), week.monday().plusDays(day), GOAL);
            }
            activity(owner.id(), week.monday().minusDays(1), 5 * GOAL); // la semaine précédente ne compte pas
            activity(me, week.monday(), GOAL - 1);
            activity(me, week.monday().plusDays(1), GOAL + 1);
            xp(owner.id(), 30);
            xp(me, 500);
        });
        return new Group(groupId, owner, week);
    }

    private void configure(Group group, LeaderboardVisibility visibility) {
        settingsService.update(
                group.owner(), group.id(), new GroupSettings(List.of(), List.of(), Set.of(), GOAL, visibility));
    }

    private static String leaderboard(UUID groupId) {
        return "/api/v1/groups/" + groupId + "/leaderboard";
    }

    private static UUID myId() {
        return UUID.fromString(given().get("/api/v1/me").then().extract().path("id"));
    }

    private static ZoneId myTimezone() {
        return ZoneId.of(given().get("/api/v1/me").then().extract().path("timezone"));
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

    private void activity(UUID userId, LocalDate day, int cards) {
        entityManager
                .createNativeQuery("""
                        insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                        values (:userId, :day, :cards, false)
                        """)
                .setParameter("userId", userId)
                .setParameter("day", day)
                .setParameter("cards", cards)
                .executeUpdate();
    }

    private void xp(UUID userId, int amount) {
        entityManager
                .createNativeQuery("""
                        insert into points_ledger (id, user_id, amount, reason, source_id, occurred_at)
                        values (gen_random_uuid(), :userId, :amount, 'REVIEW', gen_random_uuid(), :now)
                        """)
                .setParameter("userId", userId)
                .setParameter("amount", amount)
                .setParameter("now", clock.instant())
                .executeUpdate();
    }
}
