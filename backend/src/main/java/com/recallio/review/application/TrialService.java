package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.ActiveCard;
import com.recallio.content.CardDirectory.CardFilter;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.review.domain.TrialUnavailableException;
import com.recallio.review.domain.track.Readiness;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
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
 */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class TrialService {

    /** Le nombre de questions d'un essai : de quoi se faire une idée en deux minutes. */
    static final int QUESTIONS = 5;

    /** Des formats corrigés par le serveur, auxquels on répond vite : pas d'exercice de code dans un essai. */
    static final Set<CardType> FORMATS = Set.of(CardType.TRUE_FALSE, CardType.MULTIPLE_CHOICE, CardType.FILL_IN);

    private final CardDirectory cards;
    private final PracticeService practice;

    public TrialService(CardDirectory cards, PracticeService practice) {
        this.cards = cards;
        this.practice = practice;
    }

    /**
     * Les questions de l'essai d'un sujet, dans l'ordre où les poser.
     *
     * @throws TrialUnavailableException si le sujet n'a pas de notion essentielle avec une question corrigée (un sujet
     *     décrit par un document Markdown n'a pas de notions)
     */
    public List<TrialQuestion> questions(String subjectSlug) {
        Map<UUID, NotionInfo> essentials = cards.notions(Set.of(subjectSlug)).stream()
                .filter(notion -> notion.priority() == Readiness.ESSENTIAL_PRIORITY)
                .collect(Collectors.toMap(NotionInfo::id, Function.identity()));
        // La première carte de chaque notion, dans l'ordre du programme.
        Map<UUID, UUID> cardOfNotion = new HashMap<>();
        for (ActiveCard card : cards.activeCards(new CardFilter(Set.of(subjectSlug), null, Set.of(), FORMATS))) {
            UUID notion = card.notionId();
            if (notion != null && essentials.containsKey(notion)) {
                cardOfNotion.putIfAbsent(notion, card.cardId());
            }
        }
        List<NotionInfo> picked = essentials.values().stream()
                .filter(notion -> cardOfNotion.containsKey(notion.id()))
                .sorted(Comparator.comparing(NotionInfo::level).thenComparingInt(NotionInfo::position))
                .limit(QUESTIONS)
                .toList();
        if (picked.isEmpty()) {
            throw new TrialUnavailableException(subjectSlug);
        }
        // Le filtre plus haut garantit que chaque notion retenue a sa carte.
        List<UUID> cardIds = picked.stream()
                .map(notion -> Objects.requireNonNull(cardOfNotion.get(notion.id())))
                .toList();
        Map<UUID, CardSnapshot> snapshots = cards.currentVersions(cardIds).stream()
                .collect(Collectors.toMap(CardSnapshot::cardId, Function.identity()));
        List<TrialQuestion> questions = new ArrayList<>();
        for (int index = 0; index < picked.size(); index++) {
            CardSnapshot card = snapshots.get(cardIds.get(index));
            if (card != null) {
                questions.add(new TrialQuestion(picked.get(index).slug(), card));
            }
        }
        return questions;
    }

    /**
     * Corrige une réponse de l'essai, sans l'enregistrer : la même correction que l'entraînement sur une notion.
     *
     * @see PracticeService#check(String, String, GradedAnswer)
     */
    public AnswerCorrection check(String subjectSlug, String notionSlug, GradedAnswer answer) {
        return practice.check(subjectSlug, notionSlug, answer);
    }

    /** Une question de l'essai, et la notion à donner pour la corriger. */
    public record TrialQuestion(String notionSlug, CardSnapshot card) {}
}
