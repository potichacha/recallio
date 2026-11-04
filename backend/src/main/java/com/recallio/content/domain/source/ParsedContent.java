package com.recallio.content.domain.source;

import com.recallio.content.TourPage;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.NotionLevel;
import com.recallio.content.domain.Subject;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Résultat brut de la lecture d'une source, avant toute comparaison avec la base.
 *
 * @param subject le sujet décrit par l'en-tête du document (ou le fichier {@code subject.yaml} de son dossier)
 * @param notions les notions du référentiel du sujet (ADR 0026) ; aucune pour un document Markdown
 */
public record ParsedContent(
        Subject subject, List<ParsedTheme> themes, List<ParsedNotion> notions, List<SourceIssue> issues) {

    public ParsedContent {
        themes = List.copyOf(themes);
        notions = List.copyOf(notions);
        issues = List.copyOf(issues);
    }

    /** Un document Markdown : des thèmes et des cartes, sans notions. */
    public ParsedContent(Subject subject, List<ParsedTheme> themes, List<SourceIssue> issues) {
        this(subject, themes, List.of(), issues);
    }

    public int cardCount() {
        return themes.stream().mapToInt(theme -> theme.cards().size()).sum();
    }

    /**
     * Un thème : un chapitre ou le glossaire d'un document Markdown, un domaine du référentiel d'un dossier de sujet.
     *
     * @param key clé stable dans la source, dont dérive l'identifiant avec le sujet (le numéro de chapitre,
     *     {@code glossary}, ou l'identifiant du domaine)
     * @param slug identifiant du thème dans les URL et les réglages de groupe, unique dans tout le catalogue
     */
    public record ParsedTheme(String key, String slug, String title, int position, List<ParsedCard> cards) {

        public ParsedTheme {
            cards = List.copyOf(cards);
        }
    }

    /**
     * @param sourceLine ligne de la question dans la source, pour le rapport d'import ({@code 0} si la source n'en a
     *     pas)
     * @param key la clé explicite de la carte dans sa notion ; {@code null} pour un document Markdown, dont les cartes
     *     sont identifiées par leur question
     */
    public record ParsedCard(
            CardContent content, int sourceLine, @Nullable CardKey key) {

        public ParsedCard(CardContent content, int sourceLine) {
            this(content, sourceLine, null);
        }
    }

    /**
     * Identité explicite d'une carte (ADR 0026) : sa notion et sa clé dans cette notion. Corriger le texte garde la
     * carte ; changer la clé en crée une autre.
     */
    public record CardKey(String notion, String key) {}

    /**
     * Une notion du référentiel (ADR 0025) : le grain du contenu, auquel se rattachent les cartes.
     *
     * @param slug identifiant définitif dans le sujet
     * @param domain identifiant du domaine, qui est aussi la clé de son thème
     * @param position ordre dans le référentiel du sujet
     * @param lesson la courte leçon de la notion, en Markdown ; facultative tant que la matrice n'est pas contrôlée
     * @param tour les pages de la notion dans le Tour (ADR 0037) ; aucune pour une notion hors du Tour
     */
    public record ParsedNotion(
            String slug,
            String domain,
            String chapter,
            String chapterTitle,
            String title,
            NotionLevel level,
            int priority,
            List<String> requires,
            int position,
            @Nullable String lesson,
            List<TourPage> tour) {

        public ParsedNotion {
            requires = List.copyOf(requires);
            tour = List.copyOf(tour);
        }

        /** Une notion hors du Tour. */
        public ParsedNotion(
                String slug,
                String domain,
                String chapter,
                String chapterTitle,
                String title,
                NotionLevel level,
                int priority,
                List<String> requires,
                int position,
                @Nullable String lesson) {
            this(slug, domain, chapter, chapterTitle, title, level, priority, requires, position, lesson, List.of());
        }
    }

    /** Une ligne que le parseur n'a pas su interpréter, ou une incohérence (question sans réponse...). */
    public record SourceIssue(int line, String reason, String excerpt) {}
}
