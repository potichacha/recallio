package com.recallio.notification.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import com.recallio.notification.FakePushSender;
import com.recallio.notification.application.ReminderService;
import com.recallio.notification.domain.PushPorts.Delivery;
import com.recallio.notification.domain.PushSubscription;
import com.recallio.notification.domain.Reminder;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Les rappels (ADR 0047) : s'abonner sur un appareil, puis un rappel par jour au plus, à partir de l'heure choisie, tant
 * que l'objectif du jour n'est pas atteint. En test, l'horloge est fixée à midi (UTC) et l'objectif vaut 2 cartes.
 */
@QuarkusTest
class RemindersTest {

    private static final String SUBSCRIPTION = "/api/v1/me/push-subscription";
    private static final String FCM = "https://fcm.googleapis.com/fcm/send/";

    @Inject
    ReminderService reminders;

    @Inject
    FakePushSender pushes;

    @Inject
    EntityManager entityManager;

    @Inject
    Clock clock;

    @BeforeEach
    void forgetPreviousPushes() {
        pushes.reset(Delivery.SENT);
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("delete from push_subscription")
                        .executeUpdate());
    }

    @Test
    void shouldRequireAuthenticationToSubscribe() {
        subscribe(FCM + "anonymous", 9).statusCode(401);
        given().get("/api/v1/push/public-key").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "reminders-once-a-day"))
    void shouldRemindOnceADayFromTheChosenHourWhileTheGoalIsNotReached() {
        given().get("/api/v1/push/public-key").then().statusCode(200).body("publicKey", is("BFakeVapidPublicKey"));
        subscribe(FCM + "alice", 9).statusCode(204);
        subscribe(FCM + "alice-late", 23).statusCode(204);

        assertThat(reminders.sendDue()).isEqualTo(1);
        assertThat(pushes.sent()).hasSize(1);
        assertThat(pushes.sent().getFirst().subscription().endpoint()).isEqualTo(FCM + "alice");
        assertThat(pushes.sent().getFirst().reminder().title()).isEqualTo("Tes cartes du jour t'attendent");
        assertThat(pushes.sent().getFirst().reminder().url()).isEqualTo("/review");

        assertThat(reminders.sendDue()).isZero();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "reminders-goal-reached"))
    void shouldNotRemindSomeoneWhoReachedTodaysGoal() {
        subscribe(FCM + "busy", 0).statusCode(204);
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("""
                        insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                        select id, :today, 2, true from app_user where oidc_subject = 'reminders-goal-reached'
                        """)
                        .setParameter("today", LocalDate.now(clock))
                        .executeUpdate());

        assertThat(reminders.sendDue()).isZero();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "reminders-gone"))
    void shouldForgetADeviceThatUnsubscribedFromItsBrowser() {
        subscribe(FCM + "gone", 0).statusCode(204);
        pushes.reset(Delivery.GONE);

        assertThat(reminders.sendDue()).isZero();

        assertThat(subscriptionCount()).isZero();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "reminders-rules"))
    void shouldOnlyAcceptKnownPushServicesAndLetTheUserUnsubscribe() {
        subscribe("http://localhost:8080/steal", 9)
                .statusCode(422)
                .body("code", is("notification.invalid-subscription"));
        subscribe("https://evil.example/fcm.googleapis.com", 9).statusCode(422);
        subscribe(FCM + "bob", 24).statusCode(400);
        subscribe(FCM + "bob", 9).statusCode(204);

        given().queryParam("endpoint", FCM + "bob").delete(SUBSCRIPTION).then().statusCode(204);

        assertThat(subscriptionCount()).isZero();
    }

    @Test
    void shouldWordTheReminderAroundTheStreakAndWaitForTheChosenHour() {
        assertThat(Reminder.forStreak(12).title()).isEqualTo("🔥 Ta série de 12 jours t'attend");
        assertThat(Reminder.forStreak(1).title()).isEqualTo("🔥 Ta série de 1 jour t'attend");
        ZonedDateTime evening = ZonedDateTime.of(2026, 11, 5, 19, 30, 0, 0, ZoneOffset.UTC);
        assertThat(Reminder.isDue(19, evening, null)).isTrue();
        assertThat(Reminder.isDue(20, evening, null)).isFalse();
        assertThat(Reminder.isDue(19, evening, evening.toLocalDate())).isFalse();
        assertThat(PushSubscription.isPushService("https://web.push.apple.com/abc"))
                .isTrue();
        assertThat(PushSubscription.isPushService("https://fcm.googleapis.com.evil.example/x"))
                .isFalse();
    }

    private static ValidatableResponse subscribe(String endpoint, int hour) {
        return given().contentType(ContentType.JSON)
                .body("""
                        {"endpoint": "%s", "p256dh": "BKey", "auth": "secret", "reminderHour": %d}
                        """.formatted(endpoint, hour))
                .put(SUBSCRIPTION)
                .then();
    }

    private long subscriptionCount() {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery("select count(*) from push_subscription")
                                .getSingleResult())
                        .longValue());
    }
}
