package com.recallio.gamification.domain.beta;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Le rapport de la page « Bêta » des admins (ADR 0076) : ce que font les testeurs, en agrégé, d'après ce que
 * l'application garde déjà. Des comptes et des parts, jamais une personne ; le contenu (un sujet, une carte), lui, est
 * nommé.
 *
 * @param days l'activité de chaque jour de la fenêtre, du plus ancien au plus récent
 * @param subjects les sujets, le plus révisé d'abord
 * @param failedCards les cartes au plus fort taux d'échec, la plus ratée d'abord
 * @param reportedCards les cartes aux signalements ouverts les plus nombreux, la plus signalée d'abord
 */
public record BetaReport(
        Window window,
        List<DayActivity> days,
        List<SubjectActivity> subjects,
        List<FailedCard> failedCards,
        List<ReportedCard> reportedCards,
        TourUsage tour,
        FeatureUsage features) {

    /** La fenêtre mesurée, en jours, aujourd'hui compris : un mois, de quoi voir une habitude. */
    public static final int DAYS = 30;

    /** En dessous, une carte n'entre pas dans les plus ratées : une seule réponse fausse ferait 100 % d'échec. */
    public static final int MIN_ANSWERS = 5;

    /** Le nombre de cartes ratées montrées. */
    public static final int FAILED_CARDS = 20;

    /** Le nombre de cartes signalées montrées. */
    public static final int REPORTED_CARDS = 10;

    /** Le fuseau de la fenêtre : « aujourd'hui » est celui du serveur, comme pour la rétention (ADR 0050). */
    public static final ZoneId ZONE = ZoneOffset.UTC;

    private static final int PERCENT = 100;

    public BetaReport {
        days = List.copyOf(days);
        subjects = List.copyOf(subjects);
        failedCards = List.copyOf(failedCards);
        reportedCards = List.copyOf(reportedCards);
    }

    /**
     * {@code part} rapporté à {@code whole}, en pourcentage arrondi vers le bas ; aucun quand {@code whole} est nul :
     * une part de rien n'est pas 0 %.
     */
    static @Nullable Integer percent(long part, long whole) {
        return whole == 0 ? null : Math.toIntExact(part * PERCENT / whole);
    }

    /**
     * Les {@link #DAYS} derniers jours du calendrier UTC.
     *
     * @param first le premier jour
     * @param last le dernier jour : aujourd'hui
     */
    public record Window(LocalDate first, LocalDate last) {

        public static Window endingAt(Instant now) {
            LocalDate today = LocalDate.ofInstant(now, ZONE);
            return new Window(today.minusDays(DAYS - 1), today);
        }

        /** Le premier instant de la fenêtre (inclus). */
        public Instant from() {
            return first.atStartOfDay(ZONE).toInstant();
        }

        /** Le premier instant après la fenêtre (exclu) : demain à minuit. */
        public Instant to() {
            return last.plusDays(1).atStartOfDay(ZONE).toInstant();
        }

        /** Chaque jour de la fenêtre, du plus ancien au plus récent. */
        public List<LocalDate> days() {
            return Stream.iterate(first, day -> !day.isAfter(last), day -> day.plusDays(1))
                    .toList();
        }
    }
}
