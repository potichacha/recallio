package com.recallio.content.infrastructure.folder;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import com.recallio.content.infrastructure.folder.FolderFiles.CardEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.ChoiceEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.NotionFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Vérifie et convertit les cartes du fichier d'une notion. Une carte fautive est signalée et ignorée, les autres sont
 * importées.
 */
final class NotionCards {

    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    /** Les formats que l'application sait déjà présenter, et leur type de carte. */
    private static final Map<String, CardType> SUPPORTED = Map.of(
            "course", CardType.FLASHCARD,
            "multiple-choice", CardType.MULTIPLE_CHOICE,
            "code-output", CardType.CODE_OUTPUT,
            "scenario", CardType.OPEN_SCENARIO,
            "definition", CardType.DEFINITION,
            "true-false", CardType.TRUE_FALSE,
            "bug-fix", CardType.BUG_FIX,
            "fill-in", CardType.FILL_IN);

    private final Referential.Entry notion;
    private final String path;
    private final List<SourceIssue> issues;

    /** @param path le chemin du fichier, relatif à {@code notions/}, pour le rapport */
    NotionCards(Referential.Entry notion, String path, List<SourceIssue> issues) {
        this.notion = notion;
        this.path = "notions/" + path;
        this.issues = issues;
    }

    List<ParsedCard> read(NotionFile file) {
        List<ParsedCard> cards = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (CardEntry card : file.cards()) {
            String problem = problem(card, keys);
            if (problem != null) {
                issues.add(new SourceIssue(0, path + " : " + problem, card.key()));
                continue;
            }
            try {
                cards.add(new ParsedCard(
                        content(card), 0, new CardKey(notion.notion().slug(), card.key())));
            } catch (IllegalArgumentException e) {
                issues.add(new SourceIssue(0, path + " : " + e.getMessage(), card.key()));
            }
        }
        return cards;
    }

    /** Ce qui empêche d'importer la carte, ou {@code null} si elle est valide. */
    private @Nullable String problem(CardEntry card, Set<String> keys) {
        if (!KEY.matcher(card.key()).matches()) {
            return "clé invalide (minuscules, chiffres et tirets)";
        }
        if (!keys.add(card.key())) {
            return "clé en double dans la notion";
        }
        if (!notion.formats().contains(card.format())) {
            return Referential.ALL_FORMATS.contains(card.format())
                    ? "format « " + card.format() + " » non retenu pour cette notion par le référentiel"
                    : "format inconnu « " + card.format() + " »";
        }
        if (!SUPPORTED.containsKey(card.format())) {
            return "format « " + card.format() + " » pas encore pris en charge par l'application";
        }
        if (card.question().isBlank() || card.answer().isBlank()) {
            return "question ou réponse vide";
        }
        CardType type = SUPPORTED.get(card.format());
        if ((type == CardType.MULTIPLE_CHOICE) != (card.choices() != null)) {
            return type == CardType.MULTIPLE_CHOICE ? "un QCM a besoin de ses choix" : "seul un QCM a des choix";
        }
        if ((type == CardType.TRUE_FALSE) != (card.truth() != null)) {
            return type == CardType.TRUE_FALSE
                    ? "un vrai ou faux a besoin de sa valeur (truth: true ou false)"
                    : "seul un vrai ou faux a une valeur (truth)";
        }
        if ((type == CardType.FILL_IN) != (card.blanks() != null)) {
            return type == CardType.FILL_IN
                    ? "un code à trous a besoin des réponses de ses trous (blanks)"
                    : "seul un code à trous a des trous (blanks)";
        }
        return null;
    }

    private static CardContent content(CardEntry card) {
        String question = card.question().strip();
        String answer = card.answer().strip();
        List<List<String>> blanks = card.blanks();
        if (blanks != null) {
            return new CardContent.FillIn(
                    question, answer, blanks.stream().map(CardBlank::new).toList());
        }
        Boolean truth = card.truth();
        if (truth != null) {
            return new CardContent.TrueFalse(question, answer, truth);
        }
        List<ChoiceEntry> choices = card.choices();
        if (choices != null) {
            return new CardContent.MultipleChoice(
                    question,
                    answer,
                    choices.stream()
                            .map(choice -> new CardChoice(choice.text().strip(), choice.correct()))
                            .toList());
        }
        CardType type = SUPPORTED.get(card.format());
        if (type == null) {
            throw new IllegalArgumentException("format non pris en charge : " + card.format());
        }
        return CardContent.of(type, question, answer);
    }
}
