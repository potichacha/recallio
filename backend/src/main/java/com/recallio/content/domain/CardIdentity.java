package com.recallio.content.domain;

import com.recallio.content.CardChoice;
import com.recallio.platform.domain.StableUuid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identifiants stables du contenu (ADR 0009).
 *
 * <p>L'identifiant d'une carte dérive de son sujet, de son thème et de sa question <em>normalisée</em> : casse, accents,
 * ponctuation et mise en forme Markdown sont ignorés. Le sujet en fait partie (ADR 0013) : le chapitre 1 de Java et celui
 * de Go sont deux thèmes distincts, même avec des questions identiques.
 *
 * <p>Corriger une faute de frappe ou ajouter du gras garde donc la même carte (et la progression des utilisateurs) ;
 * seule une réécriture du sens de la question crée une nouvelle carte.
 *
 * <p>Les cartes d'un dossier de sujet ont une clé explicite (ADR 0026) : leur identifiant dérive de la notion et de la
 * clé, jamais du texte ({@link #keyedCardId}).
 */
public final class CardIdentity {

    private static final String SUBJECT_NAMESPACE = "recallio:subject";
    private static final String THEME_NAMESPACE = "recallio:theme";
    private static final String CARD_NAMESPACE = "recallio:card";
    private static final String NOTION_NAMESPACE = "recallio:notion";
    private static final String KEYED_CARD_NAMESPACE = "recallio:keyed-card";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    /** Marqueurs Markdown d'emphase et de code : supprimés sans laisser d'espace ({@code **JVM**} → {@code JVM}). */
    private static final Pattern MARKDOWN_MARKERS = Pattern.compile("[*`_]");
    /**
     * Tout ce qui n'est ni lettre, ni chiffre, ni opérateur devient un séparateur. Les opérateurs sont conservés : sans
     * eux, {@code `>>` vs `>>>` ?} et {@code `&` vs `&&` ?} se réduiraient tous deux à « vs ».
     */
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}<>=+\\-/%&|^~!@#$\\\\]+");

    private CardIdentity() {}

    public static UUID subjectId(String subjectSlug) {
        return StableUuid.of(SUBJECT_NAMESPACE, subjectSlug);
    }

    /** @param themeKey clé du thème dans son document : numéro de chapitre ({@code 07}) ou {@code glossary} */
    public static UUID themeId(String subjectSlug, String themeKey) {
        return StableUuid.of(THEME_NAMESPACE, qualified(subjectSlug, themeKey));
    }

    public static UUID cardId(String subjectSlug, String themeKey, String prompt) {
        return StableUuid.of(CARD_NAMESPACE, qualified(subjectSlug, themeKey) + '|' + normalize(prompt));
    }

    public static UUID notionId(String subjectSlug, String notionSlug) {
        return StableUuid.of(NOTION_NAMESPACE, qualified(subjectSlug, notionSlug));
    }

    /**
     * Identifiant d'une carte à clé explicite (ADR 0026) : il ne dépend que du sujet, de la notion et de la clé, pas du
     * texte. Reformuler la question garde la carte.
     */
    public static UUID keyedCardId(String subjectSlug, String notionSlug, String key) {
        return StableUuid.of(KEYED_CARD_NAMESPACE, qualified(subjectSlug, notionSlug) + '#' + key);
    }

    /**
     * Slug d'un thème, unique dans tout le catalogue : préfixé par le sujet, sauf si le titre commence déjà par lui ou
     * n'est que lui ({@code JAVA : FONDAMENTAUX} donne {@code java-fondamentaux}, {@code COLLECTIONS} donne
     * {@code java-collections}, {@code GIT} dans le sujet Git donne {@code git}).
     */
    public static String themeSlug(String subjectSlug, String title) {
        String local = normalize(title).replace(' ', '-');
        return local.equals(subjectSlug) || local.startsWith(subjectSlug + "-") ? local : subjectSlug + "-" + local;
    }

    private static String qualified(String subjectSlug, String themeKey) {
        return subjectSlug + '/' + themeKey;
    }

    /**
     * Forme canonique d'un texte pour la comparaison : {@code "Qu'est-ce que **la JVM** ?"} → {@code "qu est-ce que la
     * jvm"}.
     */
    public static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
        String withoutAccents = DIACRITICS.matcher(decomposed).replaceAll("");
        String withoutMarkdown = MARKDOWN_MARKERS.matcher(withoutAccents).replaceAll("");
        return SEPARATORS
                .matcher(withoutMarkdown.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .strip();
    }

    /**
     * Empreinte exacte du contenu : toute modification du texte (même de forme) crée une nouvelle version. Les choix
     * d'un QCM en font partie ; les autres formats n'en ont pas, et leur empreinte ne change pas.
     */
    public static String contentHash(CardContent content) {
        StringBuilder canonical = new StringBuilder()
                .append(content.type())
                .append('\u0000')
                .append(content.prompt())
                .append('\u0000')
                .append(content.answer());
        for (CardChoice choice : content.choices()) {
            canonical
                    .append('\u0000')
                    .append(choice.correct() ? "[x] " : "[ ] ")
                    .append(choice.text());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 est garanti par la plateforme Java", e);
        }
    }
}
