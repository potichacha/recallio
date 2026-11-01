package com.recallio.content.domain;

import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import java.util.List;

/**
 * Contenu d'une carte, selon son format. Les textes sont en Markdown.
 *
 * <p>L'interface est {@code sealed} : un {@code switch} sur ses variantes est vérifié exhaustif par le compilateur, ce
 * qui garantit qu'aucun format n'est oublié quand on en ajoute un.
 */
public sealed interface CardContent {

    /** Le recto : ce qu'on montre avant de révéler la réponse. */
    String prompt();

    /** Le verso : la réponse attendue ou la réponse modèle ; pour un QCM, l'explication montrée après la correction. */
    String answer();

    /** Les choix d'un QCM ; aucun pour les autres formats. */
    default List<CardChoice> choices() {
        return List.of();
    }

    default CardType type() {
        return switch (this) {
            case Flashcard _ -> CardType.FLASHCARD;
            case CodeOutput _ -> CardType.CODE_OUTPUT;
            case OpenScenario _ -> CardType.OPEN_SCENARIO;
            case Definition _ -> CardType.DEFINITION;
            case MultipleChoice _ -> CardType.MULTIPLE_CHOICE;
        };
    }

    record Flashcard(String prompt, String answer) implements CardContent {}

    /** {@code prompt} contient le code (bloc Markdown) dont il faut prédire la sortie. */
    record CodeOutput(String prompt, String answer) implements CardContent {}

    record OpenScenario(String prompt, String answer) implements CardContent {}

    /** {@code prompt} est le terme, {@code answer} sa définition. */
    record Definition(String prompt, String answer) implements CardContent {}

    /** Au moins deux choix, dont au moins un correct ; plusieurs peuvent l'être. */
    record MultipleChoice(String prompt, String answer, List<CardChoice> choices) implements CardContent {

        public MultipleChoice {
            choices = List.copyOf(choices);
            if (choices.size() < CardChoice.MIN_PER_CARD
                    || choices.size() > CardChoice.MAX_PER_CARD
                    || choices.stream().noneMatch(CardChoice::correct)) {
                throw new IllegalArgumentException("Un QCM a de 2 à 8 choix, dont au moins un correct");
            }
        }
    }

    /** Un contenu sans choix ; un QCM se construit avec ses choix ({@link MultipleChoice}). */
    static CardContent of(CardType type, String prompt, String answer) {
        return switch (type) {
            case FLASHCARD -> new Flashcard(prompt, answer);
            case CODE_OUTPUT -> new CodeOutput(prompt, answer);
            case OPEN_SCENARIO -> new OpenScenario(prompt, answer);
            case DEFINITION -> new Definition(prompt, answer);
            case MULTIPLE_CHOICE -> throw new IllegalArgumentException("Un QCM se construit avec ses choix");
        };
    }
}
