package com.recallio.live.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Une question de la partie, figée au démarrage : un QCM du sujet (ADR 0019), tel qu'il était à cet instant.
 *
 * @param correctChoices les positions des bonnes réponses, à partir de 0 ; jamais envoyées avant la correction
 */
public record LiveQuestion(
        UUID cardId, String prompt, List<String> choices, Set<Integer> correctChoices, String explanation) {

    public LiveQuestion {
        choices = List.copyOf(choices);
        correctChoices = Set.copyOf(correctChoices);
    }

    public boolean multipleAnswers() {
        return correctChoices.size() > 1;
    }

    /** Juste si toutes les bonnes réponses sont cochées, et elles seules (ADR 0019). */
    public boolean isCorrect(Set<Integer> selected) {
        return selected.equals(correctChoices);
    }
}
