package com.recallio.content.domain.importing;

import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import java.util.List;

/** Rapport d'import (CLAUDE.md, section 5.2) : ce qui a changé, et ce que la source contient d'anormal. */
public record ImportReport(
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
