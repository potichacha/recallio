package com.recallio.content.domain;

import com.recallio.content.CardType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Les questions qui valident une notion du Tour (ADR 0037) : ses cartes corrigées par le serveur, une de chaque format
 * d'abord, {@value #MAX} au plus, rendues dans l'ordre de son fichier. La CI veut au moins {@value #MIN} cartes de ces
 * formats dans chaque notion du Tour.
 */
public final class TourQuestions {

    /** Corrigés par le serveur sans exécuter de code : les exercices viennent à la fin du chapitre. */
    public static final Set<CardType> FORMATS = Set.of(CardType.TRUE_FALSE, CardType.MULTIPLE_CHOICE, CardType.FILL_IN);

    public static final int MAX = 3;
    public static final int MIN = 2;

    private TourQuestions() {}

    /**
     * @param cards les cartes d'une notion, dans l'ordre de son fichier
     * @param type le format d'une carte
     */
    public static <C> List<C> pick(List<C> cards, Function<C, CardType> type) {
        List<C> graded = cards.stream()
                .filter(card -> FORMATS.contains(type.apply(card)))
                .toList();
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
