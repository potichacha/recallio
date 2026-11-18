package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardFilter.Grading;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.NotionInfo;
import com.recallio.review.TrialStarted;
import com.recallio.review.domain.TrialUnavailableException;
import com.recallio.review.domain.track.Readiness;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cas d'usage : l'essai sans compte (ADR 0050). Quelques questions corrigées par le serveur, une par notion essentielle,
 * des plus simples aux plus avancées, et rien d'enregistré : on voit ce que vaut l'application avant de s'inscrire.
 *
 * <p>Ses questions sont des cartes à choix ou à trous, quel que soit leur format (ADR 0063), auxquelles on répond vite :
 * pas d'exercice de code dans un essai.
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class TrialService {

    /** Le nombre de questions d'un essai : de quoi se faire une idée en deux minutes. */
    static final int QUESTIONS = 5;

    private final CardDirectory cards;
    private final PracticeService practice;
    private final Event<TrialStarted> trialStarted;
    private final Clock clock;

    public TrialService(CardDirectory cards, PracticeService practice, Event<TrialStarted> trialStarted, Clock clock) {
        this.cards = cards;
        this.practice = practice;
        this.trialStarted = trialStarted;
        this.clock = clock;
    }

    /**
     * Les questions de l'essai d'un sujet, dans l'ordre où les poser.
     *
     * @throws TrialUnavailableException si le sujet n'a pas de notion essentielle avec une question corrigée (un sujet
     *     décrit par un document Markdown n'a pas de notions)
     */
    public List<TrialQuestion> questions(String subjectSlug) {
        List<Pick> picks = picks(subjectSlug);
        if (picks.isEmpty()) {
            throw new TrialUnavailableException(subjectSlug);
        }
        Map<UUID, CardSnapshot> snapshots = cards
                .currentVersions(picks.stream().map(Pick::cardId).toList())
                .stream()
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        List<TrialQuestion> questions = new ArrayList<>();
        for (Pick pick : picks) {
            CardSnapshot card = snapshots.get(pick.cardId());
            if (card != null) {
                questions.add(new TrialQuestion(pick.notionSlug(), card));
            }
        }
        return questions;
    }

    /**
     * Corrige une réponse de l'essai, sans l'enregistrer : la même correction que l'entraînement sur une notion. La
     * réponse à la première question annonce un essai commencé (ADR 0064) : charger les questions ne suffit pas, la
     * page publique les charge pour chaque visiteur.
     *
     * @see PracticeService#check(String, String, GradedAnswer)
     */
    public AnswerCorrection check(String subjectSlug, String notionSlug, GradedAnswer answer) {
        AnswerCorrection correction = practice.check(subjectSlug, notionSlug, answer);
        if (isFirstQuestion(subjectSlug, answer.cardId())) {
            trialStarted.fire(new TrialStarted(subjectSlug, clock.instant()));
        }
        return correction;
    }

    private boolean isFirstQuestion(String subjectSlug, UUID cardId) {
        return picks(subjectSlug).stream()
                .findFirst()
                .map(first -> first.cardId().equals(cardId))
                .orElse(false);
    }

    /** Les questions de l'essai, sans leur texte : la notion et la carte de chacune, dans l'ordre où les poser. */
    private List<Pick> picks(String subjectSlug) {
        Map<UUID, NotionInfo> essentials = cards.notions(Set.of(subjectSlug)).stream()
                .filter(notion -> notion.priority() == Readiness.ESSENTIAL_PRIORITY)
                .collect(Collectors.toMap(NotionInfo::id, Function.identity()));
        // La première carte de chaque notion, dans l'ordre du programme.
        Map<UUID, UUID> cardOfNotion = new HashMap<>();
        var filter = new CardFilter(Set.of(subjectSlug), null, Set.of(), Set.of(), Grading.CHOICES_OR_BLANKS);
        for (ActiveCard card : cards.activeCards(filter)) {
            UUID notion = card.notionId();
            if (notion != null && essentials.containsKey(notion)) {
                cardOfNotion.putIfAbsent(notion, card.cardId());
            }
        }
        // Le filtre garantit que chaque notion retenue a sa carte.
        return essentials.values().stream()
                .filter(notion -> cardOfNotion.containsKey(notion.id()))
                .sorted(Comparator.comparing(NotionInfo::level).thenComparingInt(NotionInfo::position))
                .limit(QUESTIONS)
                .map(notion -> new Pick(notion.slug(), Objects.requireNonNull(cardOfNotion.get(notion.id()))))
                .toList();
    }

    private record Pick(String notionSlug, UUID cardId) {}

    /** Une question de l'essai, et la notion à donner pour la corriger. */
    public record TrialQuestion(String notionSlug, CardSnapshot card) {}
}
