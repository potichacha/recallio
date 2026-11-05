package com.recallio.review.domain.practice;

import com.recallio.content.CardType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * L'ordre des cartes d'un entraînement sur une notion (ADR 0038). Un fichier de notion range ses cartes par format :
 * quatre vrai ou faux, puis quatre QCM… Les enchaîner ainsi rendrait la séance monotone, et la quatrième question d'un
 * format ressemblerait trop à la troisième. Les formats alternent donc, dans l'ordre où ils apparaissent, chacun dans
 * l'ordre du fichier, et les exercices de code, plus longs, viennent en dernier.
 */
public final class PracticeOrder {

    private PracticeOrder() {}

    public static <C> List<C> arrange(List<C> cards, Function<C, CardType> type) {
        Map<CardType, Deque<C>> byFormat = new LinkedHashMap<>();
        List<C> exercises = new ArrayList<>();
        for (C card : cards) {
            CardType format = type.apply(card);
            if (format == CardType.CODING_EXERCISE) {
                exercises.add(card);
            } else {
                byFormat.computeIfAbsent(format, _ -> new ArrayDeque<>()).add(card);
            }
        }
        List<C> arranged = new ArrayList<>();
        while (!byFormat.isEmpty()) {
            Iterator<Deque<C>> formats = byFormat.values().iterator();
            while (formats.hasNext()) {
                Deque<C> format = formats.next();
                arranged.add(format.removeFirst());
                if (format.isEmpty()) {
                    formats.remove();
                }
            }
        }
        arranged.addAll(exercises);
        return arranged;
    }
}
