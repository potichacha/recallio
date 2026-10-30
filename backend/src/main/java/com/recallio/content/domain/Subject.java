package com.recallio.content.domain;

/**
 * Un sujet du catalogue (CLAUDE.md, section 2.5 ; ADR 0013) : Java, Go, Kubernetes... Chaque sujet a son propre
 * document de référence, qui le décrit dans son en-tête.
 *
 * @param slug identifiant stable ({@code java}, {@code go}) : il entre dans l'identité des thèmes et des cartes, il ne
 *     change jamais
 * @param position ordre d'affichage dans le catalogue
 */
public record Subject(String slug, String title, Category category, int position, Status status) {

    /** Lettres minuscules, chiffres et tirets : le slug apparaît dans les URL et les identifiants. */
    private static final String SLUG_PATTERN = "[a-z0-9]+(-[a-z0-9]+)*";

    public Subject {
        if (!slug.matches(SLUG_PATTERN)) {
            throw new IllegalArgumentException("Slug de sujet invalide : « " + slug + " » (exemple : spring-boot)");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("Le titre du sujet est obligatoire");
        }
    }

    public boolean isPublished() {
        return status == Status.PUBLISHED;
    }

    /** Liste fermée (CLAUDE.md, section 2.5) : en ajouter une passe par un ADR. Stockée telle quelle en base. */
    public enum Category {
        LANGUAGES,
        FRAMEWORKS,
        CLOUD_DEVOPS,
        DATA,
        FUNDAMENTALS,
        AI
    }

    /** Un brouillon est importé mais invisible des utilisateurs, le temps de le relire. */
    public enum Status {
        DRAFT,
        PUBLISHED
    }
}
