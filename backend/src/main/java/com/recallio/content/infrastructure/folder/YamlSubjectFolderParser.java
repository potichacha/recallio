package com.recallio.content.infrastructure.folder;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.source.ContentCoverage;
import com.recallio.content.domain.source.ContentCoverage.NotionTarget;
import com.recallio.content.domain.source.InvalidSourceException;
import com.recallio.content.domain.source.ParsedContent;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.ParsedNotion;
import com.recallio.content.domain.source.ParsedContent.ParsedTheme;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import com.recallio.content.domain.source.SubjectFolder;
import com.recallio.content.domain.source.SubjectFolderParser;
import com.recallio.content.infrastructure.folder.FolderFiles.DomainFile;
import com.recallio.content.infrastructure.folder.FolderFiles.NotionFile;
import com.recallio.content.infrastructure.folder.FolderFiles.SubjectFile;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Lit le dossier d'un sujet (ADR 0026) : {@code subject.yaml}, le référentiel ({@code referentiel/*.yaml}) et un
 * fichier par notion ({@code notions/<domaine>/<notion>.yaml}).
 *
 * <ul>
 *   <li>un thème par domaine du référentiel qui a au moins une carte, de slug {@code <sujet>-<domaine>} : il ne change
 *       pas si le titre du domaine change ;
 *   <li>toutes les notions du référentiel, avec ou sans cartes (l'inventaire du sujet) ;
 *   <li>les cartes de chaque domaine dans l'ordre du référentiel, puis dans l'ordre de leur fichier.
 * </ul>
 *
 * <p>Un en-tête ou un référentiel illisible empêche l'import du sujet ; un fichier de notion fautif est signalé dans le
 * rapport, et seules ses cartes valides sont importées.
 */
@ApplicationScoped
public class YamlSubjectFolderParser implements SubjectFolderParser {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @Override
    public ParsedContent parse(SubjectFolder folder) {
        Subject subject = subject(read(folder.subject(), SubjectFile.class, "subject.yaml"));
        Referential referential = referential(folder);
        List<SourceIssue> issues = new ArrayList<>();
        Map<String, NotionFile> files = notionFiles(folder, referential, issues);
        Map<String, List<ParsedCard>> exercises = new NotionExercises(referential, issues).read(folder.exercises());
        List<ParsedNotion> notions = new ArrayList<>();
        Map<String, ThemeCards> themes = new LinkedHashMap<>();
        for (Referential.Entry entry : referential.entries()) {
            NotionFile file = files.get(entry.notion().slug());
            notions.add(withLesson(entry.notion(), file));
            List<ParsedCard> cards = new ArrayList<>();
            if (file != null) {
                cards.addAll(new NotionCards(entry, path(entry), issues).read(file));
            }
            addExercises(cards, exercises.getOrDefault(entry.notion().slug(), List.of()), issues);
            if (!cards.isEmpty()) {
                themes.computeIfAbsent(entry.notion().domain(), _ -> new ThemeCards(entry))
                        .cards
                        .addAll(cards);
            }
        }
        return new ParsedContent(
                subject,
                themes.values().stream()
                        .map(theme -> theme.build(subject.slug()))
                        .toList(),
                notions,
                issues);
    }

    /**
     * La couverture du référentiel par les cartes valides du dossier (ADR 0025), publiée par la CI.
     *
     * @throws InvalidSourceException si l'en-tête ou le référentiel est illisible
     */
    public ContentCoverage coverage(SubjectFolder folder) {
        ParsedContent parsed = parse(folder);
        // Les notions lues, avec leur leçon ; le référentiel seul donne les formats et la déclaration « complète ».
        Map<String, ParsedNotion> withLessons =
                parsed.notions().stream().collect(Collectors.toMap(ParsedNotion::slug, Function.identity()));
        return ContentCoverage.of(
                parsed.subject().slug(),
                referential(folder).entries().stream()
                        .map(entry -> new NotionTarget(
                                withLessons.getOrDefault(entry.notion().slug(), entry.notion()),
                                NotionCards.types(entry.formats()),
                                entry.complete()))
                        .toList(),
                parsed.themes().stream()
                        .flatMap(theme -> theme.cards().stream())
                        .toList());
    }

    private static Referential referential(SubjectFolder folder) {
        return Referential.of(new TreeMap<>(folder.referential())
                .entrySet().stream()
                        .map(file -> read(file.getValue(), DomainFile.class, "referentiel/" + file.getKey()))
                        .toList());
    }

    /**
     * Ajoute les exercices d'une notion après ses cartes. Un exercice et une carte de même clé auraient le même
     * identifiant : l'exercice est alors refusé.
     */
    private static void addExercises(List<ParsedCard> cards, List<ParsedCard> exercises, List<SourceIssue> issues) {
        Set<CardKey> keys =
                cards.stream().map(ParsedCard::key).filter(Objects::nonNull).collect(Collectors.toSet());
        for (ParsedCard exercise : exercises) {
            CardKey key = Objects.requireNonNull(exercise.key());
            if (keys.contains(key)) {
                issues.add(new SourceIssue(
                        0,
                        "exercices : la clé « %s » est déjà celle d'une carte de la notion « %s »"
                                .formatted(key.key(), key.notion()),
                        key.key()));
            } else {
                cards.add(exercise);
            }
        }
    }

    /** Les fichiers de notion lisibles, par notion ; les autres deviennent des anomalies du rapport. */
    private static Map<String, NotionFile> notionFiles(
            SubjectFolder folder, Referential referential, List<SourceIssue> issues) {
        Map<String, NotionFile> files = new LinkedHashMap<>();
        new TreeMap<>(folder.notions()).forEach((path, content) -> {
            try {
                NotionFile file = read(content, NotionFile.class, "notions/" + path);
                Referential.Entry entry = referential.find(file.notion());
                if (entry == null) {
                    issues.add(
                            new SourceIssue(0, "notions/" + path + " : notion absente du référentiel", file.notion()));
                } else if (!path.equals(path(entry))) {
                    issues.add(new SourceIssue(
                            0, "notions/" + path + " : le fichier doit s'appeler notions/" + path(entry), path));
                } else {
                    files.put(file.notion(), file);
                }
            } catch (InvalidSourceException e) {
                issues.add(new SourceIssue(0, e.getMessage() == null ? path : e.getMessage(), path));
            }
        });
        return files;
    }

    /** Chemin attendu du fichier d'une notion, relatif à {@code notions/} : {@code <domaine>/<notion>.yaml}. */
    static String path(Referential.Entry entry) {
        return entry.notion().domain() + "/" + entry.notion().slug() + ".yaml";
    }

    private static ParsedNotion withLesson(ParsedNotion notion, @Nullable NotionFile file) {
        String lesson = file == null || file.lesson() == null || file.lesson().isBlank()
                ? null
                : file.lesson().strip();
        return new ParsedNotion(
                notion.slug(),
                notion.domain(),
                notion.chapter(),
                notion.chapterTitle(),
                notion.title(),
                notion.level(),
                notion.priority(),
                notion.requires(),
                notion.position(),
                lesson);
    }

    private static Subject subject(SubjectFile file) {
        try {
            return new Subject(
                    file.subject(),
                    file.title(),
                    enumValue(Subject.Category.class, file.category()),
                    file.position(),
                    enumValue(Subject.Status.class, file.status()));
        } catch (IllegalArgumentException e) {
            throw new InvalidSourceException("subject.yaml invalide : " + e.getMessage());
        }
    }

    /** {@code cloud-devops} → {@code CLOUD_DEVOPS}, comme dans l'en-tête d'un document Markdown. */
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    private static <T> T read(String content, Class<T> type, String path) {
        try {
            return YAML.readValue(content, type);
        } catch (JacksonException e) {
            JsonLocation location = e.getLocation();
            String line = location == null ? "" : " (ligne " + location.getLineNr() + ")";
            throw new InvalidSourceException(path + " illisible" + line + " : " + e.getOriginalMessage());
        }
    }

    /** Les cartes d'un domaine, qui deviendra un thème. */
    private static final class ThemeCards {
        private final Referential.Entry first;
        private final List<ParsedCard> cards = new ArrayList<>();

        ThemeCards(Referential.Entry first) {
            this.first = first;
        }

        ParsedTheme build(String subjectSlug) {
            String domain = first.notion().domain();
            return new ParsedTheme(
                    domain, subjectSlug + "-" + domain, first.domainTitle(), first.domainPosition(), cards);
        }
    }
}
