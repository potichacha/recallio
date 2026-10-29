package com.recallio.review.application;

import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Quota quotidien de nouvelles cartes : il repart à zéro à minuit, dans le fuseau de l'utilisateur. */
@ApplicationScoped
public class NewCardQuota {

    private final ReviewStateRepository states;
    private final ReviewConfig config;

    public NewCardQuota(ReviewStateRepository states, ReviewConfig config) {
        this.states = states;
        this.config = config;
    }

    /** Nouvelles cartes encore autorisées aujourd'hui. */
    public int remainingToday(UserRef user, Instant now) {
        long introducedToday = states.countIntroducedSince(user.id(), startOfDay(now, user.timezone()));
        return (int) Math.max(0, config.newCardsPerDay() - introducedToday);
    }

    static Instant startOfDay(Instant now, ZoneId timezone) {
        return LocalDate.ofInstant(now, timezone).atStartOfDay(timezone).toInstant();
    }

    /** Minuit suivant : les cartes dues avant cet instant sont « à revoir aujourd'hui ». */
    static Instant startOfNextDay(Instant now, ZoneId timezone) {
        return LocalDate.ofInstant(now, timezone)
                .plusDays(1)
                .atStartOfDay(timezone)
                .toInstant();
    }
}
