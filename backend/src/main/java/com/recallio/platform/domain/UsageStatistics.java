package com.recallio.platform.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ce qu'un module sait de l'usage de l'application, pour la page « Bêta » des admins (ADR 0076). Chaque module qui
 * garde de quoi répondre implémente ce port et ne lit que ses tables, sur le patron de {@link PersonalData} ; le
 * rapport rassemble les réponses sans connaître une seule table.
 *
 * <p>Une question à laquelle un module n'a rien à répondre garde sa réponse vide. Une période va de {@code from} inclus
 * à {@code to} exclu. Les identifiants d'utilisateur ne servent qu'à ne pas compter deux fois un même compte d'un
 * module à l'autre : ils ne sortent pas du backend.
 */
public interface UsageStatistics {

    /**
     * Les comptes qui ont utilisé chaque fonctionnalité du module sur la période. Pour une fonctionnalité qui n'est pas
     * datée ({@link UsageFeature#isDated()}), ceux qui l'ont à ce jour, quelle que soit la période.
     */
    default Map<UsageFeature, Set<UUID>> featureUsers(Instant from, Instant to) {
        return Map.of();
    }

    /** Ce qui se compte jour par jour sur la période ; un jour sans rien est absent. */
    default Map<DailyCount, Map<LocalDate, Long>> dailyCounts(Instant from, Instant to) {
        return Map.of();
    }

    /** Ce qui se compte sujet par sujet, par slug ; un sujet sans rien est absent. */
    default Map<SubjectCount, Map<String, Long>> subjectCounts(Instant from, Instant to) {
        return Map.of();
    }

    /**
     * Les cartes au plus fort taux d'échec sur la période, la plus ratée d'abord, parmi celles qui ont reçu au moins
     * {@code minAnswers} réponses dont une fausse.
     */
    default List<CardAnswers> mostFailedCards(Instant from, Instant to, int minAnswers, int limit) {
        return List.of();
    }

    /** Le nombre de signalements ouverts de chacune de ces cartes ; une carte sans signalement ouvert est absente. */
    default Map<UUID, Long> openReports(Collection<UUID> cardIds) {
        return Map.of();
    }

    /** Les cartes qui ont le plus de signalements ouverts, la plus signalée d'abord. */
    default List<CardReports> mostReportedCards(int limit) {
        return List.of();
    }

    /**
     * Où chacun s'est arrêté dans le Tour : par notion, le nombre de comptes dont la dernière page ouverte lui
     * appartient, à ce jour. Seule la dernière page de chacun par sujet est gardée : il n'y a pas d'historique.
     */
    default Map<UUID, Long> tourStops() {
        return Map.of();
    }

    /** Ce qui se compte par jour. */
    enum DailyCount {
        /** Les comptes qui ont révisé au moins une carte, au jour de chacun dans son fuseau. */
        REVIEWERS,
        /** Les cartes révisées, au jour de chacun dans son fuseau. */
        REVIEWS,
        /** Les objectifs du jour atteints, au jour de chacun dans son fuseau. */
        GOALS_REACHED,
        /** Les comptes créés, au jour UTC. */
        ACCOUNTS_CREATED,
        /** Les notions validées dans le Tour, au jour UTC. */
        TOUR_VALIDATIONS
    }

    /** Ce qui se compte par sujet. */
    enum SubjectCount {
        /** Les comptes qui suivent le sujet, à ce jour. */
        FOLLOWERS,
        /** Les comptes qui ont révisé au moins une carte du sujet sur la période. */
        REVIEWERS,
        /** Les réponses données aux cartes du sujet sur la période. */
        REVIEWS,
        /** Celles de ces réponses qui sont justes. */
        RECALLED
    }

    /**
     * @param answers les réponses données à la carte sur la période
     * @param failures celles qui sont fausses
     */
    record CardAnswers(UUID cardId, long answers, long failures) {}

    /** @param openReports les signalements de la carte qui ne sont pas encore traités */
    record CardReports(UUID cardId, long openReports) {}
}
