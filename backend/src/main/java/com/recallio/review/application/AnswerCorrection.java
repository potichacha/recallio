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
 * La correction, par le serveur, d'une réponse à une carte à choix, quel que soit son format, ou à un code à compléter,
 * sans rien enregistrer : pour les questions du Tour (ADR 0037), l'entraînement (ADR 0038), le carnet d'erreurs, l'essai
 * et l'examen blanc.
 *
 * @param correctChoices les bonnes réponses d'une carte à choix ; aucune pour un code à trous
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
     * @throws AnswerMismatchException si la réponse ne convient pas à la carte, ou si le serveur ne la corrige pas sans
     *     exécuter de code (une carte sans choix ni trous)
     */
    public static AnswerCorrection grade(CardSnapshot card, GradedAnswer answer) {
        if (card.isGradedByChoices()) {
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
