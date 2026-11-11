package com.recallio.content.domain.source;

import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La qualité de rédaction des cartes d'un sujet (audit de novembre 2026, éléments 18 à 21) : ce qu'une relecture
 * attentive refuserait, et qu'une machine sait repérer. Chaque règle compte ses infractions ; la CI compare ces nombres
 * au plafond du sujet ({@code content/quality-baseline.yaml}), qui ne peut que baisser.
 *
 * @param subject le slug du sujet
 * @param violations les infractions, dans l'ordre des cartes
 */
public record ContentQuality(String subject, List<Violation> violations) {

    /** Une règle de rédaction ; {@code key} est son nom dans le fichier des plafonds. */
    public enum Rule {
        /** Le bon choix se repère à sa longueur : il dépasse nettement le plus long des mauvais. */
        LONGEST_CORRECT_CHOICE("longest-correct-choice"),
        /** L'application tutoie : ni « vous », ni impératif en « -ez ». */
        FORMAL_ADDRESS("formal-address"),
        /** La même question, dans le même format, deux fois dans le sujet. */
        DUPLICATE_QUESTION("duplicate-question"),
        /** Trop de QCM à plusieurs bonnes réponses : ce qui dépasse la part permise. */
        MULTIPLE_ANSWERS("multiple-answers");

        private final String key;

        Rule(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    /**
     * @param card la carte : {@code notion/clé}, ou sa ligne dans un document Markdown
     * @param detail ce qui ne va pas, pour qui la corrige
     */
    public record Violation(Rule rule, String card, String detail) {}

    /** Un bon choix ne dépasse pas le plus long des mauvais de plus de la moitié... */
    static final double MAX_LENGTH_RATIO = 1.5;

    /** ... sauf de quelques caractères : entre « int » et « String », la longueur ne trahit rien. */
    static final int MIN_LENGTH_GAP = 20;

    /** Un QCM sur quatre, au plus, a plusieurs bonnes réponses. */
    static final int MAX_MULTIPLE_ANSWERS_PERCENT = 25;

    private static final int PERCENT = 100;
    private static final int EXCERPT_LENGTH = 60;

    private static final Pattern CODE_BLOCK = Pattern.compile("(?s)```.*?```");
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`]*`");

    /** « vous », « votre », « vos », ou un verbe à la deuxième personne du pluriel. */
    private static final Pattern FORMAL =
            Pattern.compile("(?iu)(?<![\\p{L}])(vous|votre|vos|\\p{L}{2,}ez)(?![\\p{L}])");

    /** Des mots en « -ez » qui ne sont pas des verbes conjugués. */
    private static final Set<String> NOT_VERBS = Set.of("assez", "chez", "rez");

    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final Pattern ACCENTS = Pattern.compile("\\p{M}");

    public ContentQuality {
        violations = List.copyOf(violations);
    }

    public static ContentQuality of(ParsedContent content) {
        List<ParsedCard> cards = content.themes().stream()
                .flatMap(theme -> theme.cards().stream())
                .toList();
        List<Violation> violations = new ArrayList<>();
        Map<String, String> firstByQuestion = new HashMap<>();
        for (ParsedCard card : cards) {
            longestCorrectChoice(card).ifPresent(violations::add);
            formalAddress(card).ifPresent(violations::add);
            String question =
                    card.content().type() + " " + normalized(card.content().prompt());
            String first = firstByQuestion.putIfAbsent(question, name(card));
            if (first != null) {
                violations.add(new Violation(Rule.DUPLICATE_QUESTION, name(card), "même question que " + first));
            }
        }
        violations.addAll(multipleAnswers(cards));
        return new ContentQuality(content.subject().slug(), violations);
    }

    public int count(Rule rule) {
        return violations(rule).size();
    }

    public List<Violation> violations(Rule rule) {
        return violations.stream().filter(violation -> violation.rule() == rule).toList();
    }

    /** Sur une carte à une seule bonne réponse, hors vrai ou faux et hors choix en bloc de code. */
    private static Optional<Violation> longestCorrectChoice(ParsedCard card) {
        List<CardChoice> choices = card.content().choices();
        if (card.content().type() == CardType.TRUE_FALSE
                || choices.stream().filter(CardChoice::correct).count() != 1
                || choices.stream()
                        .anyMatch(choice -> CODE_BLOCK.matcher(choice.text()).find())) {
            return Optional.empty();
        }
        int correct = choices.stream()
                .filter(CardChoice::correct)
                .mapToInt(ContentQuality::length)
                .sum();
        int longestWrong = choices.stream()
                .filter(choice -> !choice.correct())
                .mapToInt(ContentQuality::length)
                .max()
                .orElse(0);
        if (correct <= longestWrong * MAX_LENGTH_RATIO || correct - longestWrong < MIN_LENGTH_GAP) {
            return Optional.empty();
        }
        return Optional.of(new Violation(
                Rule.LONGEST_CORRECT_CHOICE,
                name(card),
                "le bon choix fait %d caractères, le plus long des mauvais %d".formatted(correct, longestWrong)));
    }

    private static int length(CardChoice choice) {
        return choice.text().strip().length();
    }

    private static Optional<Violation> formalAddress(ParsedCard card) {
        CardContent content = card.content();
        List<String> texts = new ArrayList<>(List.of(content.prompt(), content.answer()));
        content.choices().forEach(choice -> texts.add(choice.text()));
        for (String text : texts) {
            Matcher matcher = FORMAL.matcher(prose(text));
            while (matcher.find()) {
                if (!NOT_VERBS.contains(matcher.group().toLowerCase(Locale.ROOT))) {
                    return Optional.of(new Violation(Rule.FORMAL_ADDRESS, name(card), "« " + matcher.group() + " »"));
                }
            }
        }
        return Optional.empty();
    }

    /** Le texte sans son code : un identifiant ou une chaîne d'un programme ne vouvoie personne. */
    private static String prose(String markdown) {
        return INLINE_CODE.matcher(CODE_BLOCK.matcher(markdown).replaceAll(" ")).replaceAll(" ");
    }

    /** Les QCM à plusieurs bonnes réponses au-delà de la part permise, les derniers du sujet. */
    private static List<Violation> multipleAnswers(List<ParsedCard> cards) {
        List<ParsedCard> quizzes = cards.stream()
                .filter(card -> card.content().type() == CardType.MULTIPLE_CHOICE)
                .toList();
        List<ParsedCard> multiple = quizzes.stream()
                .filter(card -> card.content().choices().stream()
                                .filter(CardChoice::correct)
                                .count()
                        > 1)
                .toList();
        int allowed = quizzes.size() * MAX_MULTIPLE_ANSWERS_PERCENT / PERCENT;
        return multiple.stream()
                .skip(allowed)
                .map(card -> new Violation(
                        Rule.MULTIPLE_ANSWERS,
                        name(card),
                        "%d QCM sur %d ont plusieurs bonnes réponses, %d au plus"
                                .formatted(multiple.size(), quizzes.size(), allowed)))
                .toList();
    }

    /**
     * La question sans casse, accents ni espaces superflus. La ponctuation reste : dans du code, elle fait toute la
     * différence entre deux questions.
     */
    private static String normalized(String question) {
        String unaccented = ACCENTS.matcher(Normalizer.normalize(question, Normalizer.Form.NFD))
                .replaceAll("");
        return SPACES.matcher(unaccented.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .strip();
    }

    private static String name(ParsedCard card) {
        CardKey key = card.key();
        if (key != null) {
            return key.notion() + "/" + key.key();
        }
        String prompt = card.content().prompt().strip();
        return "ligne %d (%s)"
                .formatted(card.sourceLine(), prompt.substring(0, Math.min(EXCERPT_LENGTH, prompt.length())));
    }
}
