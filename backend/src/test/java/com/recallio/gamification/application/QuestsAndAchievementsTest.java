package com.recallio.gamification.application;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.recallio.content.CardType;
import com.recallio.gamification.application.QuestService.QuestView;
import com.recallio.gamification.domain.achievement.Achievement;
import com.recallio.gamification.domain.quest.Quest;
import com.recallio.gamification.domain.quest.QuestBoard;
import com.recallio.gamification.domain.quest.QuestPeriod;
import com.recallio.review.CardReviewed;
import com.recallio.review.TourNotionValidated;
import com.recallio.review.TourPageOpened;
import com.recallio.user.UserRef;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Les quêtes et les succès (ADR 0040), à partir des vrais événements du module review, chacun dans sa transaction comme
 * en production. Chaque test a son propre utilisateur ; les quêtes du jour dépendent de lui, d'où {@link #userWith}.
 * En test, l'objectif du jour vaut 2 cartes.
 */
@QuarkusTest
class QuestsAndAchievementsTest {

    private static final int DAILY_GOAL = 2;

    @Inject
    Event<CardReviewed> cardReviewed;

    @Inject
    Event<TourPageOpened> pageOpened;

    @Inject
    Event<TourNotionValidated> notionValidated;

    @Inject
    QuestService quests;

    @Inject
    AchievementService achievements;

    @Inject
    EntityManager entityManager;

    @Inject
    Clock clock;

    @Test
    void shouldCompleteAQuestOnceAndAwardItsXpOnce() {
        UserRef user = userWith(Quest.DAILY_REVIEWS);
        for (int i = 0; i < Quest.DAILY_REVIEWS.target(); i++) {
            review(user, "java", CardType.FLASHCARD, false);
        }

        QuestView reviews = view(user, Quest.DAILY_REVIEWS);
        assertThat(reviews.completed()).isTrue();
        assertThat(reviews.progress()).isEqualTo(Quest.DAILY_REVIEWS.target());
        assertThat(questXp(user)).isEqualTo(Quest.DAILY_REVIEWS.xp());

        review(user, "java", CardType.FLASHCARD, false);

        assertThat(questXp(user)).isEqualTo(Quest.DAILY_REVIEWS.xp());
        assertThat(unlocked(user, Achievement.FIRST_QUEST)).isTrue();
        assertThat(unlocked(user, Achievement.FIRST_REVIEW)).isTrue();
    }

    @Test
    void shouldCountAPageOfTheTourOncePerDay() {
        UserRef user = userWith(Quest.DAILY_TOUR_PAGES);

        openPage(user, 1);
        openPage(user, 1);
        openPage(user, 2);

        assertThat(view(user, Quest.DAILY_TOUR_PAGES).progress()).isEqualTo(2);
        assertThat(view(user, Quest.DAILY_TOUR_PAGES).completed()).isFalse();
    }

    @Test
    void shouldUnlockTheAchievementsOfTheTourAndOfTheExercises() {
        UserRef user = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);

        QuarkusTransaction.requiringNew()
                .run(() ->
                        notionValidated.fire(new TourNotionValidated(user, "go", "hello-world", 10, clock.instant())));
        review(user, "java", CardType.CODING_EXERCISE, true);
        review(user, "go", CardType.TRUE_FALSE, true);
        assertThat(unlocked(user, Achievement.POLYGLOT)).isFalse();
        review(user, "kotlin", CardType.FLASHCARD, false);

        assertThat(unlocked(user, Achievement.FIRST_TOUR_NOTION)).isTrue();
        assertThat(unlocked(user, Achievement.TOUR_EXPLORER)).isTrue();
        assertThat(unlocked(user, Achievement.FIRST_EXERCISE)).isTrue();
        assertThat(unlocked(user, Achievement.POLYGLOT)).isTrue();
        assertThat(unlocked(user, Achievement.HUNDRED_REVIEWS)).isFalse();
    }

    @Test
    void shouldUnlockTheWeekStreakWhenTheGoalIsReachedTheSeventhDayInARow() {
        UserRef user = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        QuarkusTransaction.requiringNew().run(() -> {
            for (int daysAgo = 1; daysAgo <= 6; daysAgo++) {
                entityManager
                        .createNativeQuery("""
                                insert into daily_activity (user_id, local_date, cards_reviewed, goal_reached)
                                values (:userId, :day, :cards, true)
                                """)
                        .setParameter("userId", user.id())
                        .setParameter("day", today.minusDays(daysAgo))
                        .setParameter("cards", DAILY_GOAL)
                        .executeUpdate();
            }
        });

        review(user, "java", CardType.FLASHCARD, false);
        assertThat(unlocked(user, Achievement.STREAK_7)).isFalse();
        review(user, "java", CardType.FLASHCARD, false);

        assertThat(unlocked(user, Achievement.STREAK_7)).isTrue();
        assertThat(unlocked(user, Achievement.STREAK_30)).isFalse();
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "sub", value = "quests-fresh"))
    void shouldGiveAFreshUserTheirQuestsAndLockedAchievements() {
        given().get("/api/v1/me/quests")
                .then()
                .statusCode(200)
                .body("daily", hasSize(3))
                .body("weekly", hasSize(2))
                .body("daily.progress", everyItem(is(0)))
                .body("weekly.completed", everyItem(is(false)));
        given().get("/api/v1/me/achievements")
                .then()
                .statusCode(200)
                .body("", hasSize(Achievement.values().length))
                .body("unlockedAt", everyItem(nullValue()));
    }

    @Test
    void shouldRequireAuthentication() {
        given().get("/api/v1/me/quests").then().statusCode(401);
        given().get("/api/v1/me/achievements").then().statusCode(401);
    }

    /** Un utilisateur dont la quête fait partie des quêtes du jour. */
    private UserRef userWith(Quest quest) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        while (true) {
            UserRef user = new UserRef(UUID.randomUUID(), ZoneOffset.UTC);
            if (QuestBoard.of(user.id(), QuestPeriod.DAY, today).contains(quest)) {
                return user;
            }
        }
    }

    private void review(UserRef user, String subject, CardType type, boolean recalled) {
        QuarkusTransaction.requiringNew()
                .run(() -> cardReviewed.fire(new CardReviewed(
                        user,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        subject,
                        type,
                        type.isGradedByServer(),
                        recalled,
                        clock.instant())));
    }

    private void openPage(UserRef user, int page) {
        QuarkusTransaction.requiringNew()
                .run(() -> pageOpened.fire(new TourPageOpened(user, "go", "hello-world", page, clock.instant())));
    }

    private QuestView view(UserRef user, Quest quest) {
        return quests.board(user).daily().stream()
                .filter(view -> view.quest() == quest)
                .findFirst()
                .orElseThrow();
    }

    private boolean unlocked(UserRef user, Achievement achievement) {
        return achievements.all(user).stream()
                .anyMatch(view -> view.achievement() == achievement && view.unlockedAt() != null);
    }

    private long questXp(UserRef user) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery("""
                                        select coalesce(sum(amount), 0) from points_ledger
                                        where user_id = :userId and reason = 'QUEST'
                                        """)
                                .setParameter("userId", user.id())
                                .getSingleResult())
                        .longValue());
    }
}
