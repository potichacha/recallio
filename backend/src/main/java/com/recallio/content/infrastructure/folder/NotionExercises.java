package com.recallio.content.infrastructure.folder;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.domain.CardContent;
import com.recallio.content.domain.source.ParsedContent.CardKey;
import com.recallio.content.domain.source.ParsedContent.ParsedCard;
import com.recallio.content.domain.source.ParsedContent.SourceIssue;
import com.recallio.content.infrastructure.folder.FolderFiles.ExerciseFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Lit les exercices de code d'un dossier de sujet (ADR 0029) : un dossier par exercice,
 * {@code exercices/<domaine>/<notion>/<clé>/}, qui contient {@code exercise.yaml} (l'énoncé et l'explication), et les
 * fichiers {@code starter/}, {@code solution/} et {@code tests/}.
 *
 * <p>Comme une carte, un exercice fautif est signalé dans le rapport et ignoré ; les autres sont importés.
 */
final class NotionExercises {

    static final String FORMAT = "exercise";

    private static final String DESCRIPTION = "exercise.yaml";
    private static final String STARTER = "starter";
    private static final String SOLUTION = "solution";
    private static final String TESTS = "tests";
    /** {@code <domaine>/<notion>/<clé>/<reste>} : le dossier de l'exercice, puis un fichier dedans. */
    private static final int EXERCISE_DEPTH = 3;

    private static final Pattern KEY = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private final Referential referential;
    private final List<SourceIssue> issues;

    NotionExercises(Referential referential, List<SourceIssue> issues) {
        this.referential = referential;
        this.issues = issues;
    }

    /** Les exercices valides de chaque notion, par identifiant de notion, dans l'ordre de leur clé. */
    Map<String, List<ParsedCard>> read(Map<String, String> files) {
        Map<String, Map<String, String>> byExercise = new TreeMap<>();
        new TreeMap<>(files).forEach((path, content) -> {
            String[] segments = path.split("/", EXERCISE_DEPTH + 1);
            if (segments.length <= EXERCISE_DEPTH) {
                issues.add(new SourceIssue(
                        0,
                        "exercices/" + path + " : un exercice est un dossier exercices/<domaine>/<notion>/<clé>/",
                        path));
                return;
            }
            String exercise = String.join("/", segments[0], segments[1], segments[2]);
            byExercise.computeIfAbsent(exercise, _ -> new HashMap<>()).put(segments[EXERCISE_DEPTH], content);
        });
        Map<String, List<ParsedCard>> byNotion = new HashMap<>();
        byExercise.forEach((directory, content) -> {
            ParsedCard card = card(directory, content);
            if (card != null && card.key() != null) {
                byNotion.computeIfAbsent(card.key().notion(), _ -> new ArrayList<>())
                        .add(card);
            }
        });
        return byNotion;
    }

    private @Nullable ParsedCard card(String directory, Map<String, String> files) {
        String[] segments = directory.split("/");
        String notion = segments[1];
        String key = segments[2];
        String problem = problem(segments[0], notion, key);
        if (problem != null) {
            return reject(directory, problem);
        }
        String description = files.get(DESCRIPTION);
        if (description == null) {
            return reject(directory, "il manque " + DESCRIPTION);
        }
        try {
            ExerciseFile exercise = YAML.readValue(description, ExerciseFile.class);
            if (exercise.statement().isBlank() || exercise.explanation().isBlank()) {
                return reject(directory, "énoncé ou explication vide");
            }
            var content = new CardContent.CodingExercise(
                    exercise.statement().strip(), exercise.explanation().strip(), files(files));
            return new ParsedCard(content, 0, new CardKey(notion, key));
        } catch (JacksonException e) {
            return reject(directory, DESCRIPTION + " illisible : " + e.getOriginalMessage());
        } catch (IllegalArgumentException e) {
            return reject(directory, String.valueOf(e.getMessage()));
        }
    }

    /** Ce qui empêche d'importer l'exercice d'après son emplacement, ou {@code null} s'il est bien placé. */
    private @Nullable String problem(String domain, String notion, String key) {
        Referential.Entry entry = referential.find(notion);
        if (entry == null) {
            return "notion « " + notion + " » absente du référentiel";
        }
        if (!entry.notion().domain().equals(domain)) {
            return "la notion « %s » est dans le domaine « %s »"
                    .formatted(notion, entry.notion().domain());
        }
        if (!KEY.matcher(key).matches()) {
            return "clé invalide (minuscules, chiffres et tirets)";
        }
        if (!entry.formats().contains(FORMAT)) {
            return "format « " + FORMAT + " » non retenu pour cette notion par le référentiel";
        }
        return null;
    }

    /** Les fichiers de l'exercice, rangés par partie ; un fichier hors des trois parties est refusé. */
    private static ExerciseFiles files(Map<String, String> files) {
        List<SourceFile> starter = new ArrayList<>();
        List<SourceFile> solution = new ArrayList<>();
        List<SourceFile> tests = new ArrayList<>();
        new TreeMap<>(files).forEach((path, content) -> {
            String[] split = path.split("/", 2);
            if (path.equals(DESCRIPTION)) {
                return;
            }
            List<SourceFile> part = split.length < 2
                    ? null
                    : switch (split[0]) {
                        case STARTER -> starter;
                        case SOLUTION -> solution;
                        case TESTS -> tests;
                        default -> null;
                    };
            if (part == null) {
                throw new IllegalArgumentException("fichier « " + path + " » hors de starter/, solution/ et tests/");
            }
            part.add(new SourceFile(split[1], content));
        });
        return new ExerciseFiles(starter, solution, tests);
    }

    private @Nullable ParsedCard reject(String directory, String reason) {
        issues.add(new SourceIssue(0, "exercices/" + directory + " : " + reason, directory));
        return null;
    }
}
