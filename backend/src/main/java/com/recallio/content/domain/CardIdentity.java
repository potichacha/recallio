package com.recallio.content.domain;

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
 * <p>L'identifiant d'une carte dérive de son thème et de sa question <em>normalisée</em> : casse, accents, ponctuation et
 * mise en forme Markdown sont ignorés. Corriger une faute de frappe ou ajouter du gras garde donc la même carte (et la
 * progression des utilisateurs) ; seule une réécriture du sens de la question crée une nouvelle carte.
 */
public final class CardIdentity {

    private static final String THEME_NAMESPACE = "recallio:theme";
    private static final String CARD_NAMESPACE = "recallio:card";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    /** Marqueurs Markdown d'emphase et de code : supprimés sans laisser d'espace ({@code **JVM**} → {@code JVM}). */
    private static final Pattern MARKDOWN_MARKERS = Pattern.compile("[*`_]");
    /**
     * Tout ce qui n'est ni lettre, ni chiffre, ni opérateur devient un séparateur. Les opérateurs sont conservés : sans
     * eux, {@code `>>` vs `>>>` ?} et {@code `&` vs `&&` ?} se réduiraient tous deux à « vs ».
     */
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}<>=+\\-/%&|^~!@#$\\\\]+");

    private CardIdentity() {}

    public static UUID themeId(String themeKey) {
        return StableUuid.of(THEME_NAMESPACE, themeKey);
    }

    public static UUID cardId(String themeKey, String prompt) {
        return StableUuid.of(CARD_NAMESPACE, themeKey + '|' + normalize(prompt));
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

    /** Empreinte exacte du contenu : toute modification du texte (même de forme) crée une nouvelle version. */
    public static String contentHash(CardContent content) {
        String canonical = content.type() + "\u0000" + content.prompt() + "\u0000" + content.answer();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 est garanti par la plateforme Java", e);
        }
    }
}
