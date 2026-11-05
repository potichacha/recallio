package com.recallio.review.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Une réponse que le serveur corrige sans l'enregistrer : une question du Tour (ADR 0037) ou une carte de l'entraînement
 * (ADR 0038).
 *
 * @param selectedChoices les positions cochées d'un QCM ou d'un vrai ou faux, à partir de 0 ; absentes sinon
 * @param filledBlanks les réponses d'un code à trous, une par trou ; absentes sinon
 */
public record GradedAnswer(
        UUID cardId,
        @Nullable Set<Integer> selectedChoices,
        @Nullable List<String> filledBlanks) {

    public GradedAnswer {
        selectedChoices = selectedChoices == null ? null : Set.copyOf(selectedChoices);
        filledBlanks = filledBlanks == null ? null : List.copyOf(filledBlanks);
    }
}
