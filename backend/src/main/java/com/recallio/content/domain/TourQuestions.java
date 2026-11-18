package com.recallio.content.domain;

import com.recallio.content.CardType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Les questions qui valident une notion du Tour (ADR 0037) : celles de ses cartes que le serveur corrige sans exécuter
 * de code (des choix ou des trous, ADR 0063), une de chaque format d'abord, {@value #MAX} au plus, rendues dans l'ordre
 * de son fichier. Les exercices viennent à la fin du chapitre. La CI veut au moins {@value #MIN} de ces cartes dans
 * chaque notion du Tour.
 */
public final class TourQuestions {

    public static final int MAX = 3;
    public static final int MIN = 2;

    private TourQuestions() {}

    /**
     * @param cards les cartes d'une notion, dans l'ordre de son fichier
     * @param type le format d'une carte
     * @param askable vrai pour une carte que le serveur corrige sans exécuter de code
     */
    public static <C> List<C> pick(List<C> cards, Function<C, CardType> type, Predicate<C> askable) {
        List<C> graded = cards.stream().filter(askable).toList();
        List<C> picked = new ArrayList<>();
        Set<CardType> formats = EnumSet.noneOf(CardType.class);
        for (C card : graded) {
            if (picked.size() < MAX && formats.add(type.apply(card))) {
                picked.add(card);
            }
        }
        // Moins de trois formats : les cartes suivantes complètent, toujours dans l'ordre du fichier.
        for (C card : graded) {
            if (picked.size() < MAX && !picked.contains(card)) {
                picked.add(card);
            }
        }
        return graded.stream().filter(picked::contains).toList();
    }
}
