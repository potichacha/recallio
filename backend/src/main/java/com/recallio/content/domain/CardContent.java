package com.recallio.content.domain;

/**
 * Contenu d'une carte, selon son format. Les textes sont en Markdown.
 *
 * <p>L'interface est {@code sealed} : un {@code switch} sur ses variantes est vérifié exhaustif par le compilateur, ce
 * qui garantit qu'aucun format n'est oublié quand on en ajoute un (par exemple le QCM en phase 2).
 */
public sealed interface CardContent {

    /** Le recto : ce qu'on montre avant de révéler la réponse. */
    String prompt();

    /** Le verso : la réponse attendue ou la réponse modèle. */
    String answer();

    default CardType type() {
        return switch (this) {
            case Flashcard _ -> CardType.FLASHCARD;
            case CodeOutput _ -> CardType.CODE_OUTPUT;
            case OpenScenario _ -> CardType.OPEN_SCENARIO;
            case Definition _ -> CardType.DEFINITION;
        };
    }

    record Flashcard(String prompt, String answer) implements CardContent {}

    /** {@code prompt} contient le code (bloc Markdown) dont il faut prédire la sortie. */
    record CodeOutput(String prompt, String answer) implements CardContent {}

    record OpenScenario(String prompt, String answer) implements CardContent {}

    /** {@code prompt} est le terme, {@code answer} sa définition. */
    record Definition(String prompt, String answer) implements CardContent {}

    static CardContent of(CardType type, String prompt, String answer) {
        return switch (type) {
            case FLASHCARD -> new Flashcard(prompt, answer);
            case CODE_OUTPUT -> new CodeOutput(prompt, answer);
            case OPEN_SCENARIO -> new OpenScenario(prompt, answer);
            case DEFINITION -> new Definition(prompt, answer);
        };
    }
}
