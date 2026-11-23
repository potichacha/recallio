package com.recallio.platform.infrastructure;

import java.util.regex.Pattern;

/**
 * Masque, dans un texte, ce qu'une adresse de l'API porte de secret ou de personnel, avant qu'il ne quitte le serveur
 * (ADR 0070) : le jeton d'un lien d'invitation, l'adresse d'un profil public, et toute chaîne de requête, retirée. Les
 * UUID des ressources (parties, duels, défis) restent : ils ne disent rien de personne.
 *
 * <p>Par exemple, le gestionnaire d'erreurs de Quarkus écrit « HTTP Request to /api/v1/invitations/&lt;jeton&gt;/accept
 * failed » : le jeton devient {@value #TOKEN}.
 */
final class SensitivePaths {

    /** Ce qui remplace le jeton d'une invitation, comme dans le chemin de la ressource. */
    static final String TOKEN = ":token";

    /** Ce qui remplace l'adresse d'un profil public. */
    static final String HANDLE = ":handle";

    /** Un segment de chemin : il s'arrête au segment suivant, à la chaîne de requête, à une espace ou à un guillemet. */
    private static final String SEGMENT = "[^/?#\\s\"']+";

    private static final Pattern INVITATION_TOKEN = Pattern.compile("(/invitations/)" + SEGMENT);
    private static final Pattern PUBLIC_PROFILE_HANDLE = Pattern.compile("(/public-profiles/)" + SEGMENT);

    /**
     * Une chaîne de requête suit un caractère de l'adresse, jamais une espace : le « ? » d'une phrase (« Pourquoi ? »)
     * reste en place.
     */
    private static final Pattern QUERY_STRING = Pattern.compile("(?<=[^\\s?])\\?[^\\s\"']+");

    private SensitivePaths() {}

    /** Le texte, jeton et adresse de profil masqués, chaîne de requête retirée. */
    static String mask(String text) {
        String masked = QUERY_STRING.matcher(text).replaceAll("");
        masked = INVITATION_TOKEN.matcher(masked).replaceAll("$1" + TOKEN);
        return PUBLIC_PROFILE_HANDLE.matcher(masked).replaceAll("$1" + HANDLE);
    }
}
