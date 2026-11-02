package com.recallio.content.infrastructure.folder;

import com.recallio.content.domain.source.SubjectFolder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Trouve et lit les dossiers de sujet sur le disque ({@code content/<sujet>/subject.yaml}, ADR 0026). */
public final class SubjectFolders {

    public static final String SUBJECT_FILE = "subject.yaml";
    private static final String REFERENTIAL = "referentiel";
    private static final String NOTIONS = "notions";
    private static final String EXERCISES = "exercices";
    private static final String YAML = ".yaml";

    private SubjectFolders() {}

    /** Les dossiers de sujet de {@code root}, dans l'ordre alphabétique : ceux qui ont un {@code subject.yaml}. */
    public static List<Path> in(Path root) {
        try (Stream<Path> children = Files.list(root)) {
            return children.filter(child -> Files.isRegularFile(child.resolve(SUBJECT_FILE)))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister le dossier de contenu " + root, e);
        }
    }

    public static SubjectFolder read(Path folder) {
        return new SubjectFolder(
                readString(folder.resolve(SUBJECT_FILE)),
                yamlFiles(folder.resolve(REFERENTIAL), false),
                yamlFiles(folder.resolve(NOTIONS), true),
                files(folder.resolve(EXERCISES), true, _ -> true));
    }

    /** Les fichiers YAML d'un dossier, par chemin relatif (séparateur {@code /}) ; aucun si le dossier n'existe pas. */
    private static Map<String, String> yamlFiles(Path directory, boolean recursive) {
        return files(directory, recursive, file -> file.getFileName().toString().endsWith(YAML));
    }

    /** Les fichiers retenus d'un dossier, par chemin relatif (séparateur {@code /}) ; aucun s'il n'existe pas. */
    private static Map<String, String> files(Path directory, boolean recursive, Predicate<Path> retained) {
        if (!Files.isDirectory(directory)) {
            return Map.of();
        }
        try (Stream<Path> files = recursive ? Files.walk(directory) : Files.list(directory)) {
            return files.filter(file -> Files.isRegularFile(file) && retained.test(file))
                    .collect(Collectors.toMap(
                            file -> directory.relativize(file).toString().replace('\\', '/'),
                            SubjectFolders::readString));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister " + directory, e);
        }
    }

    private static String readString(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire " + file, e);
        }
    }
}
