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

    /**
     * Le verso : la réponse attendue ou la réponse modèle ; pour une carte à choix, l'explication montrée après la
     * correction.
     */
    String answer();

    /**
     * Les choix d'une carte qui se corrige sur eux : ceux d'un QCM, « Vrai » et « Faux », ou ceux qu'a reçus une carte
     * d'un autre format (ADR 0051) ; aucun pour une carte qui s'auto-évalue, un code à trous ou un exercice.
     */
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

    /**
     * Une question de cours. Sans choix, elle s'auto-évalue ; avec, elle se corrige comme un QCM (ADR 0051), et
     * {@code answer} devient l'explication. Il en va de même des autres formats qui reçoivent des choix.
     */
    record Flashcard(String prompt, String answer, List<CardChoice> choices) implements CardContent {

        public Flashcard {
            choices = CardChoice.checkedIfAny(choices);
        }

        public Flashcard(String prompt, String answer) {
            this(prompt, answer, List.of());
        }
    }

    /** {@code prompt} contient le code (bloc Markdown) dont il faut prédire la sortie ; ses choix, des sorties. */
    record CodeOutput(String prompt, String answer, List<CardChoice> choices) implements CardContent {

        public CodeOutput {
            choices = CardChoice.checkedIfAny(choices);
        }

        public CodeOutput(String prompt, String answer) {
            this(prompt, answer, List.of());
        }
    }

    /** Une mise en situation ; ses choix, des façons d'y répondre. */
    record OpenScenario(String prompt, String answer, List<CardChoice> choices) implements CardContent {

        public OpenScenario {
            choices = CardChoice.checkedIfAny(choices);
        }

        public OpenScenario(String prompt, String answer) {
            this(prompt, answer, List.of());
        }
    }

    /** {@code prompt} est le terme, {@code answer} sa définition ; ses choix, des définitions. */
    record Definition(String prompt, String answer, List<CardChoice> choices) implements CardContent {

        public Definition {
            choices = CardChoice.checkedIfAny(choices);
        }

        public Definition(String prompt, String answer) {
            this(prompt, answer, List.of());
        }
    }

    /** Au moins deux choix, dont au moins un correct ; plusieurs peuvent l'être. */
    record MultipleChoice(String prompt, String answer, List<CardChoice> choices) implements CardContent {

        public MultipleChoice {
            choices = CardChoice.checked(choices);
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

    /**
     * {@code prompt} contient le code bogué, {@code answer} le code corrigé et l'explication du bug ; ses choix, des
     * corrections.
     */
    record BugFix(String prompt, String answer, List<CardChoice> choices) implements CardContent {

        public BugFix {
            choices = CardChoice.checkedIfAny(choices);
        }

        public BugFix(String prompt, String answer) {
            this(prompt, answer, List.of());
        }
    }

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
     * Un contenu sans choix, trous ni fichiers : il s'auto-évalue. Un QCM, un vrai ou faux, un code à trous ou un
     * exercice se construit avec eux.
     */
    static CardContent of(CardType type, String prompt, String answer) {
        return of(type, prompt, answer, List.of());
    }

    /**
     * Un contenu qui se corrige sur ses choix, ou qui s'auto-évalue s'il n'en a pas (ADR 0051).
     *
     * @throws IllegalArgumentException pour un format qui n'en reçoit pas, ou des choix invalides
     */
    static CardContent of(CardType type, String prompt, String answer, List<CardChoice> choices) {
        return switch (type) {
            case FLASHCARD -> new Flashcard(prompt, answer, choices);
            case CODE_OUTPUT -> new CodeOutput(prompt, answer, choices);
            case OPEN_SCENARIO -> new OpenScenario(prompt, answer, choices);
            case DEFINITION -> new Definition(prompt, answer, choices);
            case BUG_FIX -> new BugFix(prompt, answer, choices);
            case MULTIPLE_CHOICE -> new MultipleChoice(prompt, answer, choices);
            case TRUE_FALSE -> throw new IllegalArgumentException("Un vrai ou faux se construit avec sa valeur");
            case FILL_IN -> throw new IllegalArgumentException("Un code à trous se construit avec ses trous");
            case CODING_EXERCISE -> throw new IllegalArgumentException("Un exercice se construit avec ses fichiers");
        };
    }
}
