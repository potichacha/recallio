package com.recallio.content.domain.source;

import com.recallio.content.domain.CardContent;
import java.util.List;

/** Résultat brut de la lecture d'une source, avant toute comparaison avec la base. */
public record ParsedContent(List<ParsedTheme> themes, List<SourceIssue> issues) {

    public ParsedContent {
        themes = List.copyOf(themes);
        issues = List.copyOf(issues);
    }

    public int cardCount() {
        return themes.stream().mapToInt(theme -> theme.cards().size()).sum();
    }

    /**
     * Un thème (un chapitre ou le glossaire).
     *
     * @param key clé stable dont dérive l'identifiant (le numéro de chapitre, ou {@code glossary})
     */
    public record ParsedTheme(String key, String slug, String title, int position, List<ParsedCard> cards) {

        public ParsedTheme {
            cards = List.copyOf(cards);
        }
    }

    /** @param sourceLine ligne de la question dans la source, pour le rapport d'import */
    public record ParsedCard(CardContent content, int sourceLine) {}

    /** Une ligne que le parseur n'a pas su interpréter, ou une incohérence (question sans réponse...). */
    public record SourceIssue(int line, String reason, String excerpt) {}
}
