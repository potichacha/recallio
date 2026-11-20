package com.recallio.review.domain.level;

import com.recallio.content.NotionInfo;
import com.recallio.review.domain.level.LevelCheckExceptions.InvalidLevelCheckAnswersException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Un domaine des bases d'un sujet, tel que le test de niveau le pose (ADR 0068) : ses notions débutantes, et les
 * questions possibles, des cartes de ces notions que le serveur corrige sans exécuter de code.
 *
 * <p>Le test pose {@link #QUESTIONS_PER_DOMAIN} questions par domaine, de notions différentes, de chapitres différents
 * si possible ; un domaine dont une seule notion a de telles cartes n'en pose qu'une. Le domaine est réussi si toutes
 * ses questions sont justes : il rend alors connues <em>toutes</em> ses notions débutantes, celles qui n'ont pas été
 * posées comprises. C'est le pari du test : deux réponses justes au hasard arrivent une fois sur neuf avec trois choix.
 *
 * @param id l'identifiant du domaine dans le référentiel
 * @param title son titre
 * @param beginnerNotions toutes ses notions débutantes, avec ou sans question possible
 * @param candidates les questions possibles, dans l'ordre du référentiel
 */
public record LevelCheckDomain(String id, String title, Set<UUID> beginnerNotions, List<Candidate> candidates) {

    /** Deux questions par domaine : assez pour généraliser, assez peu pour finir le test en cinq minutes. */
    public static final int QUESTIONS_PER_DOMAIN = 2;

    public LevelCheckDomain {
        beginnerNotions = Set.copyOf(beginnerNotions);
        candidates = List.copyOf(candidates);
    }

    /**
     * Une question possible.
     *
     * @param chapter le chapitre de sa notion dans le référentiel
     */
    public record Candidate(UUID cardId, UUID notionId, String chapter) {}

    /**
     * Les domaines des bases d'un sujet, dans l'ordre du référentiel : ceux dont au moins une notion débutante a une
     * question possible.
     *
     * @param notions les notions du sujet, dans l'ordre du référentiel
     * @param gradedCards les cartes que le serveur corrige sans exécuter de code, par notion, dans l'ordre du programme
     */
    public static List<LevelCheckDomain> of(List<NotionInfo> notions, Map<UUID, List<UUID>> gradedCards) {
        Map<String, List<NotionInfo>> beginnersByDomain = new LinkedHashMap<>();
        for (NotionInfo notion : notions) {
            if (notion.level() == NotionInfo.Level.BEGINNER) {
                beginnersByDomain
                        .computeIfAbsent(notion.domain(), _ -> new ArrayList<>())
                        .add(notion);
            }
        }
        List<LevelCheckDomain> domains = new ArrayList<>();
        beginnersByDomain.forEach((domain, beginners) -> {
            List<Candidate> candidates = new ArrayList<>();
            for (NotionInfo notion : beginners) {
                gradedCards.getOrDefault(notion.id(), List.of()).stream()
                        .map(card -> new Candidate(card, notion.id(), notion.chapter()))
                        .forEach(candidates::add);
            }
            if (!candidates.isEmpty()) {
                Set<UUID> ids = new LinkedHashSet<>();
                beginners.forEach(notion -> ids.add(notion.id()));
                domains.add(new LevelCheckDomain(domain, beginners.getFirst().domainTitle(), ids, candidates));
            }
        });
        return List.copyOf(domains);
    }

    /** Le nombre de questions du domaine : deux, ou une seule si une seule de ses notions a des questions possibles. */
    public int questionCount() {
        long notions = candidates.stream().map(Candidate::notionId).distinct().count();
        return (int) Math.min(QUESTIONS_PER_DOMAIN, notions);
    }

    /** Vrai si toutes les notions débutantes du domaine sont connues : il a déjà été réussi. */
    public boolean isKnown(Set<UUID> knownNotions) {
        return knownNotions.containsAll(beginnerNotions);
    }

    /**
     * Tire les questions du domaine : une carte au hasard de chaque notion tirée, les notions au hasard, de chapitres pas
     * encore tirés d'abord.
     */
    public List<UUID> draw(RandomGenerator random) {
        Map<UUID, List<Candidate>> byNotion = new LinkedHashMap<>();
        candidates.forEach(candidate -> byNotion.computeIfAbsent(candidate.notionId(), _ -> new ArrayList<>())
                .add(candidate));
        List<List<Candidate>> notions = new ArrayList<>(byNotion.values());
        Collections.shuffle(notions, random);

        int count = questionCount();
        List<List<Candidate>> picked = new ArrayList<>();
        Set<String> chapters = new HashSet<>();
        for (List<Candidate> notion : notions) {
            if (picked.size() < count && chapters.add(notion.getFirst().chapter())) {
                picked.add(notion);
            }
        }
        for (List<Candidate> notion : notions) {
            if (picked.size() < count && !picked.contains(notion)) {
                picked.add(notion);
            }
        }
        return picked.stream()
                .map(cards -> cards.get(random.nextInt(cards.size())).cardId())
                .toList();
    }

    /**
     * Vérifie que les cartes répondues sont celles d'un tirage du domaine : autant que ses questions, chacune une
     * question possible du domaine, et chacune d'une notion différente.
     *
     * @throws InvalidLevelCheckAnswersException sinon
     */
    public void requireDrawnCards(Collection<UUID> cardIds) {
        Map<UUID, Candidate> byCard = new HashMap<>();
        candidates.forEach(candidate -> byCard.put(candidate.cardId(), candidate));
        Set<UUID> notions = new HashSet<>();
        boolean drawn = cardIds.size() == questionCount();
        for (UUID cardId : cardIds) {
            Candidate candidate = byCard.get(cardId);
            drawn &= candidate != null && notions.add(candidate.notionId());
        }
        if (!drawn) {
            throw new InvalidLevelCheckAnswersException(title, questionCount());
        }
    }
}
