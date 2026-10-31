package com.recallio.content.domain.importing;

import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import java.util.List;

/**
 * Rapport d'import d'un sujet (CLAUDE.md, section 5.2) : ce qui a changé, et ce que le document contient d'anormal.
 *
 * @param subject le slug du sujet importé
 */
public record ImportReport(
        String subject,
        int themesCreated,
        int themesUpdated,
        int cardsAdded,
        int cardsUpdated,
        int cardsUnchanged,
        int cardsRetired,
        int cardsReactivated,
        List<Duplicate> duplicates,
        List<SourceIssue> issues) {

    public ImportReport {
        duplicates = List.copyOf(duplicates);
        issues = List.copyOf(issues);
    }

    /** Deux questions du même thème qui, une fois normalisées, sont identiques : seule la première est importée. */
    public record Duplicate(String themeKey, int line, String prompt) {}
}
