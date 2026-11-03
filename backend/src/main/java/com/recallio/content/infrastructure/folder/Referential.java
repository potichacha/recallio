package com.recallio.content.infrastructure.folder;

import com.recallio.content.domain.NotionLevel;
import com.recallio.content.domain.source.InvalidSourceException;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import com.recallio.content.infrastructure.folder.FolderFiles.ChapterEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.DomainFile;
import com.recallio.content.infrastructure.folder.FolderFiles.NotionEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Le référentiel d'un sujet, lu et vérifié : ses domaines dans l'ordre, et chaque notion avec les formats qui lui
 * conviennent. Les règles de cohérence plus fines (niveau et priorité des prérequis, cycles) sont contrôlées en CI par
 * {@code ReferentialTest} ; l'import vérifie ce dont la base a besoin : identifiants uniques, prérequis existants.
 */
final class Referential {

    /** Tous les formats d'une notion, quand le référentiel ne les restreint pas (ADR 0025). */
    static final Set<String> ALL_FORMATS = Set.of(
            "course",
            "true-false",
            "multiple-choice",
            "fill-in",
            "code-output",
            "bug-fix",
            "scenario",
            "definition",
            "exercise");

    /**
     * Une notion, et ce que l'import doit savoir pour rattacher ses cartes.
     *
     * @param complete vrai si le référentiel la déclare complète (ADR 0025)
     */
    record Entry(ParsedNotion notion, String domainTitle, int domainPosition, Set<String> formats, boolean complete) {}

    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 4;

    private final Map<String, Entry> bySlug;

    private Referential(Map<String, Entry> bySlug) {
        this.bySlug = bySlug;
    }

    static Referential of(List<DomainFile> domains) {
        Map<String, Entry> bySlug = new LinkedHashMap<>();
        int position = 0;
        for (DomainFile domain : domains.stream()
                .sorted(Comparator.comparingInt(DomainFile::position))
                .toList()) {
            for (ChapterEntry chapter : domain.chapters()) {
                for (NotionEntry entry : chapter.notions()) {
                    Entry notion = toEntry(domain, chapter, entry, position++);
                    if (bySlug.put(entry.id(), notion) != null) {
                        throw invalid("la notion « " + entry.id() + " » est déclarée deux fois");
                    }
                }
            }
        }
        bySlug.values()
                .forEach(entry -> entry.notion().requires().stream()
                        .filter(required -> !bySlug.containsKey(required))
                        .findFirst()
                        .ifPresent(missing -> {
                            throw invalid("la notion « %s » requiert « %s », qui n'existe pas"
                                    .formatted(entry.notion().slug(), missing));
                        }));
        return new Referential(bySlug);
    }

    /** Les notions dans l'ordre du référentiel (domaines par position, puis ordre des fichiers). */
    List<Entry> entries() {
        return new ArrayList<>(bySlug.values());
    }

    @Nullable
    Entry find(String slug) {
        return bySlug.get(slug);
    }

    private static Entry toEntry(DomainFile domain, ChapterEntry chapter, NotionEntry entry, int position) {
        List<String> requires = entry.requires() == null ? List.of() : entry.requires();
        Set<String> formats = entry.formats() == null ? ALL_FORMATS : Set.copyOf(entry.formats());
        if (entry.priority() < MIN_PRIORITY || entry.priority() > MAX_PRIORITY) {
            throw invalid("la priorité de la notion « %s » va de 1 à 4".formatted(entry.id()));
        }
        if (!ALL_FORMATS.containsAll(formats)) {
            throw invalid("la notion « " + entry.id() + " » déclare un format inconnu : " + formats);
        }
        var notion = new ParsedNotion(
                entry.id(),
                domain.domain(),
                chapter.id(),
                chapter.title(),
                entry.title(),
                level(entry),
                entry.priority(),
                requires,
                position,
                null);
        return new Entry(notion, domain.title(), domain.position(), formats, Boolean.TRUE.equals(entry.complete()));
    }

    private static NotionLevel level(NotionEntry entry) {
        try {
            return NotionLevel.valueOf(entry.level().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw invalid("niveau inconnu « %s » pour la notion « %s »".formatted(entry.level(), entry.id()));
        }
    }

    private static InvalidSourceException invalid(String reason) {
        return new InvalidSourceException("Référentiel invalide : " + reason + ".");
    }
}
