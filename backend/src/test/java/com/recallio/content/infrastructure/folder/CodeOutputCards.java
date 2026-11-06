package com.recallio.content.infrastructure.folder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.recallio.content.infrastructure.folder.FolderFiles.CardEntry;
import com.recallio.content.infrastructure.folder.FolderFiles.NotionFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Les « qu'affiche ce code ? » des dossiers de sujet, pour les tests qui les vérifient : {@link CodeOutputTest} les
 * exécute, {@link CodeOutputChoicesTest} contrôle leurs choix.
 */
final class CodeOutputCards {

    private static final Path CONTENT = Path.of("..", "content");
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String FORMAT = "code-output";

    private CodeOutputCards() {}

    /** Les cartes qui satisfont {@code filter}, nommées par leur sujet, leur fichier et leur clé. */
    static Stream<Arguments> matching(Predicate<CardEntry> filter) {
        return SubjectFolders.in(CONTENT).stream().flatMap(folder -> {
            Map<String, String> notions = SubjectFolders.read(folder).notions();
            return notions.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .flatMap(file -> read(file.getValue()).cards().stream()
                            .filter(card -> card.format().equals(FORMAT) && filter.test(card))
                            .map(card ->
                                    Arguments.of(folder.getFileName() + "/" + file.getKey() + "#" + card.key(), card)));
        });
    }

    /**
     * La sortie aux fins de ligne près : {@code \r\n} comme {@code \n}, sans les espaces invisibles en fin de ligne, sans
     * la dernière ligne vide.
     */
    static String lines(@Nullable String output) {
        return output == null
                ? ""
                : output.replace("\r\n", "\n")
                        .lines()
                        .map(String::stripTrailing)
                        .collect(Collectors.joining("\n"))
                        .stripTrailing();
    }

    private static NotionFile read(String content) {
        try {
            return YAML.readValue(content, NotionFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
