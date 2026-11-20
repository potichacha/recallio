package com.recallio.review.domain.daily;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Les défis du jour (ADR 0067), dans les tables {@code daily_challenge}, {@code daily_challenge_question} et
 * {@code daily_challenge_answer}.
 */
public interface DailyChallengeRepository {

    Optional<DailyChallenge> find(UUID id);

    Optional<DailyChallenge> find(String subjectSlug, LocalDate day);

    /** Les défis de ces jours, tous sujets confondus. */
    List<DailyChallenge> findByDays(Collection<LocalDate> days);

    /**
     * Garde le défi s'il n'y en a pas encore pour son sujet et son jour : deux premières demandes simultanées gardent le
     * même.
     *
     * @return le défi gardé pour ce sujet et ce jour, celui-ci ou celui d'une demande arrivée avant
     */
    DailyChallenge addIfAbsent(DailyChallenge challenge);

    /** Les cartes des derniers défis du sujet, avant ce jour. */
    Set<UUID> recentCards(String subjectSlug, LocalDate before, int challenges);

    /** Les réponses du joueur à ces défis. */
    List<DailyChallengeAnswer> answers(UUID userId, Collection<UUID> challengeIds);

    Optional<DailyChallengeAnswer> findAnswer(UUID requestId);

    /** @return faux si la question a déjà la réponse du joueur, ou si l'identifiant de requête a déjà servi */
    boolean addAnswerIfAbsent(DailyChallengeAnswer answer);

    /**
     * Le nombre de joueurs de chaque score, parmi ceux qui ont répondu à chacune de ces questions du défi.
     *
     * @param positions les questions encore posées
     */
    Map<Integer, Integer> playersByScore(UUID challengeId, Collection<Integer> positions);
}
