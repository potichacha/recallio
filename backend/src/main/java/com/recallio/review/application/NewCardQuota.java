package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.review.domain.ReviewStateRepository;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quota quotidien de nouvelles cartes, propre à chaque sujet (ADR 0048) : il repart à zéro à minuit, dans le fuseau de
 * l'utilisateur.
 */
@ApplicationScoped
public class NewCardQuota {

    private final ReviewStateRepository states;
    private final CardDirectory cards;
    private final ReviewConfig config;

    public NewCardQuota(ReviewStateRepository states, CardDirectory cards, ReviewConfig config) {
        this.states = states;
        this.cards = cards;
        this.config = config;
    }

    /** Nouvelles cartes encore autorisées aujourd'hui dans chacun de ces sujets. */
    public Map<String, Integer> remainingToday(UserRef user, Collection<String> subjectSlugs, Instant now) {
        Map<String, Long> introduced = introducedTodayBySubject(user, now);
        return subjectSlugs.stream()
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(), subject -> remaining(introduced.getOrDefault(subject, 0L))));
    }

    /** Les cartes vues pour la première fois depuis minuit, comptées par sujet. */
    private Map<String, Long> introducedTodayBySubject(UserRef user, Instant now) {
        Set<UUID> introduced = states.cardsIntroducedSince(user.id(), startOfDay(now, user.timezone()));
        return cards.subjectsOf(introduced).values().stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private int remaining(long introducedToday) {
        return (int) Math.max(0, config.newCardsPerSubjectPerDay() - introducedToday);
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
