package com.recallio.ranked.domain;

import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.ranked.domain.RankedExceptions.AnswerShapeException;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;

/**
 * La correction d'une réponse de duel par le serveur (ADR 0046) : un QCM, un vrai ou faux ou un code à compléter. Les
 * règles de justesse sont celles de la carte ({@link CardSnapshot#isCorrectSelection}, {@link CardSnapshot#checkBlanks}),
 * comme en révision.
 */
public final class DuelGrading {

    private DuelGrading() {}

    /**
     * @param correctChoices les bonnes réponses d'un QCM ou d'un vrai ou faux ; aucune pour un code à trous
     * @param blanks la correction de chaque trou ; aucune pour les autres formats
     * @param explanation le verso de la carte, en Markdown
     */
    public record Correction(
            boolean correct, Set<Integer> correctChoices, List<BlankResult> blanks, String explanation) {

        public Correction {
            correctChoices = Set.copyOf(correctChoices);
            blanks = List.copyOf(blanks);
        }
    }

    public record BlankResult(String expected, boolean correct) {}

    /** @throws AnswerShapeException si la réponse ne convient pas au format de la carte */
    public static Correction grade(
            CardSnapshot card, @Nullable Set<Integer> selectedChoices, @Nullable List<String> filledBlanks) {
        if (card.isGradedByChoices()) {
            if (selectedChoices == null || filledBlanks != null) {
                throw new AnswerShapeException();
            }
            return new Correction(
                    card.isCorrectSelection(selectedChoices), card.correctChoices(), List.of(), card.answer());
        }
        if (card.type() != CardType.FILL_IN
                || filledBlanks == null
                || selectedChoices != null
                || filledBlanks.size() != card.blanks().size()) {
            throw new AnswerShapeException();
        }
        List<Boolean> checks = card.checkBlanks(filledBlanks);
        List<BlankResult> blanks = IntStream.range(0, checks.size())
                .mapToObj(index -> new BlankResult(card.blanks().get(index).expected(), checks.get(index)))
                .toList();
        return new Correction(!checks.contains(false), Set.of(), blanks, card.answer());
    }
}
