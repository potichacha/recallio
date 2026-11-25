package com.recallio.gamification.domain.beta;

import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * De quoi reconnaître une carte dans le rapport (ADR 0076). Une carte retirée, ou d'un sujet qui n'est plus publié,
 * n'a plus que son identifiant et son sujet.
 *
 * @param subjectSlug le sujet de la carte ; {@code null} si la carte est inconnue du catalogue
 * @param themeSlug son thème, pour l'ouvrir dans le catalogue ; {@code null} pour une carte retirée
 * @param notionTitle le titre de sa notion ; {@code null} si elle n'en a pas, ou pour une carte retirée
 * @param excerpt le début de sa question ; {@code null} pour une carte retirée
 * @param type son format ; {@code null} pour une carte retirée
 */
public record CardLabel(
        UUID cardId,
        @Nullable String subjectSlug,
        @Nullable String subjectTitle,
        @Nullable String themeSlug,
        @Nullable String notionTitle,
        @Nullable String excerpt,
        @Nullable CardType type) {

    /** La longueur de l'extrait d'une question : celle que voient déjà les admins dans les signalements. */
    static final int EXCERPT_LENGTH = 200;

    /**
     * @param snapshot la version courante de la carte ; {@code null} si elle est retirée, ou si son sujet n'est pas
     *     publié
     * @param subjectSlug le sujet de la carte, retirée ou non ; {@code null} si elle est inconnue
     */
    public static CardLabel of(UUID cardId, @Nullable CardSnapshot snapshot, @Nullable String subjectSlug) {
        if (snapshot == null) {
            return new CardLabel(cardId, subjectSlug, null, null, null, null, null);
        }
        return new CardLabel(
                cardId,
                snapshot.subjectSlug(),
                snapshot.subjectTitle(),
                snapshot.themeSlug(),
                snapshot.notionTitle(),
                excerpt(snapshot.prompt()),
                snapshot.type());
    }

    private static String excerpt(String prompt) {
        String flat = prompt.strip().replaceAll("\\s+", " ");
        return flat.length() <= EXCERPT_LENGTH ? flat : flat.substring(0, EXCERPT_LENGTH - 1) + "…";
    }
}
