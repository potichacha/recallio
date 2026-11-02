package com.recallio.content.infrastructure.folder;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * La forme des fichiers YAML d'un dossier de sujet (ADR 0026), telle que Jackson la lit. Une propriété inconnue ou une
 * propriété obligatoire absente fait échouer la lecture : une faute de frappe dans un nom de champ se voit.
 */
final class FolderFiles {

    private FolderFiles() {}

    /** {@code subject.yaml} : les mêmes clés que l'en-tête d'un document Markdown (ADR 0013). */
    record SubjectFile(
            @JsonProperty(required = true) String subject,
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) String category,
            @JsonProperty(required = true) int position,
            @JsonProperty(required = true) String status) {}

    /** Un fichier du référentiel : un domaine, ses chapitres et leurs notions (ADR 0025). */
    record DomainFile(
            @JsonProperty(required = true) String domain,
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) int position,
            @JsonProperty(required = true) List<ChapterEntry> chapters) {}

    record ChapterEntry(
            @JsonProperty(required = true) String id,
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) List<NotionEntry> notions) {}

    /** @param formats les formats qui conviennent à la notion ; tous si absent */
    record NotionEntry(
            @JsonProperty(required = true) String id,
            @JsonProperty(required = true) String title,
            @JsonProperty(required = true) String level,
            @JsonProperty(required = true) int priority,
            @Nullable List<String> requires,
            @Nullable List<String> formats) {}

    /** Un fichier de notion : sa leçon et ses cartes. */
    record NotionFile(
            @JsonProperty(required = true) String notion,
            @Nullable String lesson,
            @JsonProperty(required = true) List<CardEntry> cards) {}

    /**
     * @param choices les choix d'un QCM ; absents pour les autres formats
     * @param truth la valeur de l'affirmation d'un vrai ou faux ; absente pour les autres formats
     * @param blanks pour un code à trous, les réponses acceptées par chaque trou ({@code [[hashCode], [hash]]}) ;
     *     absentes pour les autres formats
     * @param check pour un « qu'affiche ce code ? », ce que la CI exécute et le résultat attendu (ADR 0028) ; ni importé
     *     ni montré
     * @param unverifiable pour un « qu'affiche ce code ? » dont le résultat n'est pas déterministe, la raison pour
     *     laquelle la CI ne l'exécute pas
     */
    record CardEntry(
            @JsonProperty(required = true) String key,
            @JsonProperty(required = true) String format,
            @JsonProperty(required = true) String question,
            @JsonProperty(required = true) String answer,
            @Nullable List<ChoiceEntry> choices,
            @Nullable Boolean truth,
            @Nullable List<List<String>> blanks,
            @Nullable CheckEntry check,
            @Nullable String unverifiable) {}

    /**
     * La vérification d'un « qu'affiche ce code ? » par la CI (ADR 0028) : soit le code ne compile pas, soit il affiche
     * {@code output} (rien si absent) puis lève éventuellement l'exception {@code throws}.
     *
     * @param code le code exécuté, quand la question n'est pas exécutable telle quelle ; sinon son bloc {@code java}
     * @param output la sortie standard attendue, exacte aux fins de ligne près
     * @param throwsName le nom simple de l'exception attendue ({@code NullPointerException})
     * @param compiles {@code false} si le code ne doit pas compiler
     */
    record CheckEntry(
            @Nullable String code,
            @Nullable String output,
            @JsonProperty("throws") @Nullable String throwsName,
            @Nullable Boolean compiles) {}

    record ChoiceEntry(
            @JsonProperty(required = true) String text,
            @JsonProperty(required = true) boolean correct) {}
}
