package com.recallio.review.application;

import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.review.application.ReviewRecordingService.BlankCorrection;
import com.recallio.review.domain.AnswerMismatchException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * La correction, par le serveur, d'une réponse à un vrai ou faux, un QCM ou un code à compléter, sans rien enregistrer :
 * pour les questions du Tour (ADR 0037) et l'entraînement (ADR 0038).
 *
 * @param correctChoices les bonnes réponses d'un QCM ou d'un vrai ou faux ; aucune pour un code à trous
 * @param blanks la correction de chaque trou d'un code à trous ; aucune pour les autres formats
 * @param explanation le verso de la carte, en Markdown
 */
public record AnswerCorrection(
        UUID cardId, boolean correct, Set<Integer> correctChoices, List<BlankCorrection> blanks, String explanation) {

    public AnswerCorrection {
        correctChoices = Set.copyOf(correctChoices);
        blanks = List.copyOf(blanks);
    }

    /**
     * Corrige la réponse donnée à cette carte.
     *
     * @throws AnswerMismatchException si la carte s'auto-évalue, ou si la réponse ne convient pas à son format
     */
    public static AnswerCorrection grade(CardSnapshot card, GradedAnswer answer) {
        if (card.type().isGradedByChoices()) {
            Set<Integer> selected = answer.selectedChoices();
            if (selected == null || answer.filledBlanks() != null) {
                throw AnswerMismatchException.choicesExpected();
            }
            return new AnswerCorrection(
                    card.cardId(), card.isCorrectSelection(selected), card.correctChoices(), List.of(), card.answer());
        }
        if (card.type() != CardType.FILL_IN) {
            throw AnswerMismatchException.notGradedByServer();
        }
        List<String> filled = answer.filledBlanks();
        if (filled == null || filled.size() != card.blanks().size() || answer.selectedChoices() != null) {
            throw AnswerMismatchException.blanksExpected(card.blanks().size());
        }
        List<Boolean> checks = card.checkBlanks(filled);
        List<BlankCorrection> blanks = IntStream.range(0, checks.size())
                .mapToObj(index -> new BlankCorrection(card.blanks().get(index).expected(), checks.get(index)))
                .toList();
        return new AnswerCorrection(card.cardId(), !checks.contains(false), Set.of(), blanks, card.answer());
    }
}
