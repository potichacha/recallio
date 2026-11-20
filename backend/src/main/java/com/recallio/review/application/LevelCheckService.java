package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardFilter.Grading;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.content.SubjectDirectory;
import com.recallio.review.domain.level.KnownNotionRepository;
import com.recallio.review.domain.level.LevelCheckDomain;
import com.recallio.review.domain.level.LevelCheckExceptions.InvalidLevelCheckAnswersException;
import com.recallio.review.domain.level.LevelCheckExceptions.LevelCheckDomainNotFoundException;
import com.recallio.review.domain.level.LevelCheckExceptions.LevelCheckUnavailableException;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cas d'usage : le test de niveau d'un sujet (ADR 0068). Deux questions par domaine des bases ; un domaine réussi rend
 * connues toutes ses notions débutantes, qui ouvrent alors le parcours « apprendre » et passent après les autres en
 * révision.
 *
 * <p>Le test est une révision personnelle : aucune mesure anti-triche (CLAUDE.md, 2.4), donc pas de session gardée.
 * Chaque domaine se corrige seul, d'après les cartes répondues, et la PWA arrête le test après deux domaines ratés de
 * suite. Ni le journal de révision, ni FSRS, ni l'XP ne voient ces réponses, et une notion connue n'est pas acquise : la
 * maîtrise et l'indice de préparation ne voient que les vraies révisions.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class LevelCheckService {

    private final CardDirectory cards;
    private final SubjectDirectory subjects;
    private final KnownNotionRepository known;
    private final Clock clock;

    public LevelCheckService(CardDirectory cards, SubjectDirectory subjects, KnownNotionRepository known, Clock clock) {
        this.cards = cards;
        this.subjects = subjects;
        this.known = known;
        this.clock = clock;
    }

    /**
     * Le test d'un sujet, tiré à chaque appel : ses domaines des bases, dans l'ordre du référentiel, chacun avec ses
     * questions.
     *
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le sujet n'est pas publié
     * @throws LevelCheckUnavailableException s'il n'a aucune notion débutante avec des questions
     */
    public LevelCheck levelCheck(UserRef user, String subjectSlug) {
        Bases bases = bases(subjectSlug);
        Set<UUID> knownNotions = known.known(user.id(), Set.of(subjectSlug));
        var random = ThreadLocalRandom.current();
        List<List<UUID>> drawn =
                bases.domains().stream().map(domain -> domain.draw(random)).toList();
        Map<UUID, CardSnapshot> questions =
                cards.currentVersions(drawn.stream().flatMap(List::stream).toList()).stream()
                        .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        List<DomainQuestions> domains = new ArrayList<>();
        for (int index = 0; index < drawn.size(); index++) {
            LevelCheckDomain domain = bases.domains().get(index);
            List<CardSnapshot> asked = new ArrayList<>();
            for (UUID cardId : drawn.get(index)) {
                CardSnapshot question = questions.get(cardId);
                if (question != null) {
                    asked.add(question);
                }
            }
            domains.add(new DomainQuestions(domain, domain.isKnown(knownNotions), asked));
        }
        return new LevelCheck(
                subjectSlug,
                subjects.publishedTitles().getOrDefault(subjectSlug, subjectSlug),
                domains,
                bases.knownCount(knownNotions));
    }

    /**
     * Corrige les réponses aux questions d'un domaine. Toutes justes, le domaine est réussi : ses notions débutantes
     * deviennent connues. Raté, rien ne change : une notion déjà connue le reste.
     *
     * @throws com.recallio.platform.domain.ResourceNotFoundException si le sujet n'est pas publié
     * @throws LevelCheckUnavailableException s'il n'a aucune notion débutante avec des questions
     * @throws LevelCheckDomainNotFoundException si le test du sujet n'a pas ce domaine
     * @throws InvalidLevelCheckAnswersException si les cartes ne sont pas celles d'un tirage du domaine
     * @throws com.recallio.review.domain.AnswerMismatchException si une réponse ne convient pas à sa question
     */
    @Transactional
    public DomainOutcome check(UserRef user, String subjectSlug, String domainId, List<GradedAnswer> answers) {
        Bases bases = bases(subjectSlug);
        LevelCheckDomain domain = bases.domains().stream()
                .filter(candidate -> candidate.id().equals(domainId))
                .findFirst()
                .orElseThrow(() -> new LevelCheckDomainNotFoundException(subjectSlug, domainId));
        List<UUID> cardIds = answers.stream().map(GradedAnswer::cardId).toList();
        domain.requireDrawnCards(cardIds);
        Map<UUID, CardSnapshot> questions = cards.currentVersions(cardIds).stream()
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        List<AnswerCorrection> corrections = new ArrayList<>();
        for (GradedAnswer answer : answers) {
            CardSnapshot question = questions.get(answer.cardId());
            if (question == null) {
                // Retirée entre la lecture des cartes du domaine et celle de leur version : comme une carte d'ailleurs.
                throw new InvalidLevelCheckAnswersException(domain.title(), domain.questionCount());
            }
            corrections.add(AnswerCorrection.grade(question, answer));
        }
        boolean passed = corrections.stream().allMatch(AnswerCorrection::correct);
        if (passed) {
            known.markKnown(user.id(), subjectSlug, domain.beginnerNotions(), clock.instant());
        }
        return new DomainOutcome(
                domain.id(), passed, corrections, bases.knownCount(known.known(user.id(), Set.of(subjectSlug))));
    }

    /** Annule le test d'un sujet : toutes ses notions connues sont oubliées. Rien à faire s'il n'en a pas. */
    @Transactional
    public void cancel(UserRef user, String subjectSlug) {
        known.forget(user.id(), subjectSlug);
    }

    /**
     * Les domaines des bases d'un sujet publié, et toutes ses notions actives.
     *
     * @throws LevelCheckUnavailableException s'il n'a aucune notion débutante avec des questions
     */
    private Bases bases(String subjectSlug) {
        subjects.requirePublished(subjectSlug);
        List<NotionInfo> notions = cards.notions(Set.of(subjectSlug));
        Map<UUID, List<UUID>> gradedCards = new HashMap<>();
        var filter = new CardFilter(Set.of(subjectSlug), null, Set.of(), Set.of(), Grading.CHOICES_OR_BLANKS);
        for (ActiveCard card : cards.activeCards(filter)) {
            UUID notion = card.notionId();
            if (notion != null) {
                gradedCards.computeIfAbsent(notion, _ -> new ArrayList<>()).add(card.cardId());
            }
        }
        List<LevelCheckDomain> domains = LevelCheckDomain.of(notions, gradedCards);
        if (domains.isEmpty()) {
            throw new LevelCheckUnavailableException(subjectSlug);
        }
        return new Bases(domains, notions.stream().map(NotionInfo::id).collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * @param domains les domaines du test, dans l'ordre du référentiel
     * @param notions toutes les notions actives du sujet
     */
    private record Bases(List<LevelCheckDomain> domains, Set<UUID> notions) {

        /** Les notions connues encore actives : une notion retirée du référentiel ne compte plus. */
        int knownCount(Set<UUID> knownNotions) {
            return (int) knownNotions.stream().filter(notions::contains).count();
        }
    }

    /**
     * Le test d'un sujet.
     *
     * @param domains ses domaines des bases, dans l'ordre du référentiel
     * @param knownNotions les notions du sujet que l'utilisateur connaît déjà
     */
    public record LevelCheck(String subjectSlug, String subjectTitle, List<DomainQuestions> domains, int knownNotions) {

        public LevelCheck {
            domains = List.copyOf(domains);
        }
    }

    /**
     * Un domaine du test et ses questions, sans leurs réponses.
     *
     * @param known vrai si toutes ses notions débutantes sont déjà connues
     */
    public record DomainQuestions(LevelCheckDomain domain, boolean known, List<CardSnapshot> questions) {

        public DomainQuestions {
            questions = List.copyOf(questions);
        }
    }

    /**
     * La correction d'un domaine.
     *
     * @param corrections dans l'ordre des réponses
     * @param knownNotions les notions du sujet connues après cette correction
     */
    public record DomainOutcome(String domain, boolean passed, List<AnswerCorrection> corrections, int knownNotions) {

        public DomainOutcome {
            corrections = List.copyOf(corrections);
        }
    }
}
