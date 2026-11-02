package com.recallio.content.domain;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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

    /** Les trous d'une carte « compléter le code » ; aucun pour les autres formats. */
    default List<CardBlank> blanks() {
        return List.of();
    }

    /** Les fichiers d'un exercice de code ; aucun pour les autres formats. */
    default Optional<ExerciseFiles> exercise() {
        return Optional.empty();
    }

    default CardType type() {
        return switch (this) {
            case Flashcard _ -> CardType.FLASHCARD;
            case CodeOutput _ -> CardType.CODE_OUTPUT;
            case OpenScenario _ -> CardType.OPEN_SCENARIO;
            case Definition _ -> CardType.DEFINITION;
            case MultipleChoice _ -> CardType.MULTIPLE_CHOICE;
            case TrueFalse _ -> CardType.TRUE_FALSE;
            case BugFix _ -> CardType.BUG_FIX;
            case FillIn _ -> CardType.FILL_IN;
            case CodingExercise _ -> CardType.CODING_EXERCISE;
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

    /**
     * Une affirmation à juger (ADR 0027) : ses deux choix, « Vrai » et « Faux », sont fixes, ce qui la corrige comme un
     * QCM.
     *
     * @param prompt l'affirmation
     * @param answer l'explication, montrée après la correction
     * @param truth vrai si l'affirmation est vraie
     */
    record TrueFalse(String prompt, String answer, boolean truth) implements CardContent {

        public static final String TRUE_LABEL = "Vrai";
        public static final String FALSE_LABEL = "Faux";

        @Override
        public List<CardChoice> choices() {
            return List.of(new CardChoice(TRUE_LABEL, truth), new CardChoice(FALSE_LABEL, !truth));
        }
    }

    /** {@code prompt} contient le code bogué, {@code answer} le code corrigé et l'explication du bug. */
    record BugFix(String prompt, String answer) implements CardContent {}

    /**
     * Du code à trous (ADR 0027) : chaque {@link CardBlank#MARKER} de la question est un trou, dans l'ordre de
     * {@code blanks}.
     *
     * @param answer l'explication, montrée après la correction
     */
    record FillIn(String prompt, String answer, List<CardBlank> blanks) implements CardContent {

        public FillIn {
            blanks = List.copyOf(blanks);
            int markers = prompt.split(Pattern.quote(CardBlank.MARKER), -1).length - 1;
            if (blanks.isEmpty() || blanks.size() > CardBlank.MAX_PER_CARD || markers != blanks.size()) {
                throw new IllegalArgumentException("Le code a %d trou(s) « %s » pour %d réponse(s) : il en faut autant,"
                                .formatted(markers, CardBlank.MARKER, blanks.size())
                        + " de 1 à " + CardBlank.MAX_PER_CARD);
            }
        }
    }

    /**
     * Un exercice de code (ADR 0029) : un énoncé, des fichiers de départ à modifier, une solution et des tests.
     *
     * @param prompt l'énoncé
     * @param answer l'explication de la solution, montrée avec elle
     */
    record CodingExercise(String prompt, String answer, ExerciseFiles files) implements CardContent {

        @Override
        public Optional<ExerciseFiles> exercise() {
            return Optional.of(files);
        }
    }

    /**
     * Un contenu sans choix, trous ni fichiers ; un QCM, un vrai ou faux, un code à trous ou un exercice se construit
     * avec eux.
     */
    static CardContent of(CardType type, String prompt, String answer) {
        return switch (type) {
            case FLASHCARD -> new Flashcard(prompt, answer);
            case CODE_OUTPUT -> new CodeOutput(prompt, answer);
            case OPEN_SCENARIO -> new OpenScenario(prompt, answer);
            case DEFINITION -> new Definition(prompt, answer);
            case BUG_FIX -> new BugFix(prompt, answer);
            case MULTIPLE_CHOICE -> throw new IllegalArgumentException("Un QCM se construit avec ses choix");
            case TRUE_FALSE -> throw new IllegalArgumentException("Un vrai ou faux se construit avec sa valeur");
            case FILL_IN -> throw new IllegalArgumentException("Un code à trous se construit avec ses trous");
            case CODING_EXERCISE -> throw new IllegalArgumentException("Un exercice se construit avec ses fichiers");
        };
    }
}
