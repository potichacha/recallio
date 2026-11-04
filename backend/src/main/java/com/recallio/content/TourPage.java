package com.recallio.content;

import com.recallio.content.ExerciseFiles.SourceFile;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Une page du Tour (ADR 0037) : un texte pour découvrir une notion, un programme à modifier et à exécuter, et parfois
 * un « À toi ». Les pages d'une notion se suivent dans l'ordre de son fichier, et toutes sont dans un même langage.
 *
 * @param title un titre court ; celui de la notion s'il est absent
 * @param text l'explication, en Markdown, écrite pour quelqu'un qui découvre le langage
 * @param task un « À toi », facultatif
 */
public record TourPage(
        @Nullable String title,
        String text,
        Program example,
        @Nullable Task task) {

    public static final int MAX_TITLE_LENGTH = 80;
    public static final int MAX_FILES = 10;

    /** Le fichier d'un programme Java qui contient {@code main} : le runner le lance (ADR 0037). */
    public static final String JAVA_ENTRY_POINT = "Main.java";

    public TourPage {
        if (title != null && (title.isBlank() || title.length() > MAX_TITLE_LENGTH)) {
            throw new IllegalArgumentException(
                    "Le titre d'une page fait de 1 à %d caractères".formatted(MAX_TITLE_LENGTH));
        }
        requireText("Le texte de la page", text);
        if (task != null && task.language() != example.language()) {
            throw new IllegalArgumentException("L'« À toi » est dans le langage de l'exemple : " + example.language());
        }
    }

    public CodeLanguage language() {
        return example.language();
    }

    /**
     * Le langage d'un programme du Tour, après avoir vérifié ses fichiers : ceux d'une page, ou ceux qu'en a faits
     * l'utilisateur avant de l'exécuter.
     *
     * @throws IllegalArgumentException si les fichiers ne forment pas un programme
     */
    public static CodeLanguage programLanguage(List<SourceFile> files) {
        requireProgram("Le programme", files);
        return languageOf(files);
    }

    /**
     * Un programme complet et la sortie qu'il donne. La CI l'exécute dans le bac à sable et compare (ADR 0037).
     *
     * @param output la sortie standard du programme, exacte aux fins de ligne près
     */
    public record Program(List<SourceFile> files, String output) {

        public Program {
            files = List.copyOf(files);
            requireProgram("L'exemple", files);
            requireText("La sortie de l'exemple", output);
        }

        public CodeLanguage language() {
            return languageOf(files);
        }
    }

    /**
     * Un « À toi » : on modifie le code de départ jusqu'à obtenir la sortie attendue. Le serveur compare la sortie du
     * programme de l'utilisateur à celle-ci ; la solution montre une façon d'y arriver.
     *
     * @param instructions la consigne, en Markdown
     * @param solution une solution, qui a exactement les fichiers du code de départ
     * @param output la sortie attendue, exacte aux fins de ligne près
     */
    public record Task(String instructions, List<SourceFile> starter, List<SourceFile> solution, String output) {

        public Task {
            starter = List.copyOf(starter);
            solution = List.copyOf(solution);
            requireText("La consigne de l'« À toi »", instructions);
            requireProgram("Le code de départ", starter);
            requireProgram("La solution", solution);
            if (!paths(starter).equals(paths(solution))) {
                throw new IllegalArgumentException("La solution a exactement les fichiers du code de départ : "
                        + paths(starter) + " contre " + paths(solution));
            }
            requireText("La sortie attendue de l'« À toi »", output);
        }

        public CodeLanguage language() {
            return languageOf(starter);
        }
    }

    private static void requireText(String what, String text) {
        if (text.isBlank()) {
            throw new IllegalArgumentException(what + " est vide");
        }
    }

    /**
     * De 1 à {@value #MAX_FILES} fichiers d'un même langage, aux chemins distincts, sans fichier de tests ; en Java, un
     * {@value #JAVA_ENTRY_POINT} à la racine.
     */
    private static void requireProgram(String part, List<SourceFile> files) {
        if (files.isEmpty() || files.size() > MAX_FILES) {
            throw new IllegalArgumentException(
                    "%s a de 1 à %d fichier(s), pas %d".formatted(part, MAX_FILES, files.size()));
        }
        if (paths(files).size() != files.size()) {
            throw new IllegalArgumentException(part + " a deux fichiers de même chemin");
        }
        CodeLanguage language = languageOf(files);
        for (SourceFile file : files) {
            if (language(file) != language) {
                throw new IllegalArgumentException("%s est écrit dans un seul langage : %s n'est pas un fichier %s"
                        .formatted(part, file.path(), language));
            }
            language.pathProblem(file.path(), false).ifPresent(problem -> {
                throw new IllegalArgumentException(part + " : " + problem);
            });
        }
        if (language == CodeLanguage.JAVA && !paths(files).contains(JAVA_ENTRY_POINT)) {
            throw new IllegalArgumentException(part + " Java a un fichier " + JAVA_ENTRY_POINT + ", qui contient main");
        }
    }

    private static CodeLanguage languageOf(List<SourceFile> files) {
        return language(files.getFirst());
    }

    /** Le chemin d'un fichier a déjà été vérifié ({@link SourceFile#PATH_PATTERN}) : son extension est connue. */
    private static CodeLanguage language(SourceFile file) {
        return CodeLanguage.of(file.path()).orElseThrow();
    }

    private static Set<String> paths(List<SourceFile> files) {
        return files.stream().map(SourceFile::path).collect(Collectors.toSet());
    }
}
