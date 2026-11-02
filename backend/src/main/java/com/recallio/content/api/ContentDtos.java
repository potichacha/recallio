package com.recallio.content.api;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.domain.Subject;
import com.recallio.content.domain.catalog.ContentCatalog.CardView;
import com.recallio.content.domain.catalog.ContentCatalog.SubjectSummary;
import com.recallio.content.domain.catalog.ContentCatalog.ThemeSummary;
import com.recallio.content.domain.importing.ImportReport;
import com.recallio.platform.domain.Page;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Contrat JSON du module content : le domaine n'est jamais exposé directement. */
final class ContentDtos {

    private ContentDtos() {}

    @Schema(name = "Subject")
    record SubjectDto(
            UUID id,

            @Schema(description = "Identifiant stable du sujet (java, go, spring-boot...)")
            String slug,

            String title,
            SubjectCategory category,
            int position,

            @Schema(description = "DRAFT n'est visible que des admins")
            SubjectStatus status,

            long themeCount,
            long cardCount) {
        static SubjectDto from(SubjectSummary subject) {
            return new SubjectDto(
                    subject.id(),
                    subject.slug(),
                    subject.title(),
                    SubjectCategory.valueOf(subject.category().name()),
                    subject.position(),
                    SubjectStatus.valueOf(subject.status().name()),
                    subject.themeCount(),
                    subject.cardCount());
        }
    }

    /** Miroir de {@link Subject.Category} dans le contrat : le nom du schéma OpenAPI reste explicite. */
    enum SubjectCategory {
        LANGUAGES,
        FRAMEWORKS,
        CLOUD_DEVOPS,
        DATA,
        FUNDAMENTALS,
        AI
    }

    /** Miroir de {@link Subject.Status} dans le contrat. */
    enum SubjectStatus {
        DRAFT,
        PUBLISHED
    }

    @Schema(name = "Theme")
    record ThemeDto(
            UUID id,

            @Schema(description = "Slug du sujet (java, go...)")
            String subjectSlug,

            String subjectTitle,
            String slug,
            String title,
            int position,
            long cardCount) {
        static ThemeDto from(ThemeSummary theme) {
            return new ThemeDto(
                    theme.id(),
                    theme.subjectSlug(),
                    theme.subjectTitle(),
                    theme.slug(),
                    theme.title(),
                    theme.position(),
                    theme.cardCount());
        }
    }

    @Schema(name = "Card")
    record CardDto(
            UUID id,
            CardType type,
            @Schema(description = "Recto, en Markdown") String prompt,

            @Schema(description = "Verso, en Markdown ; pour un QCM, l'explication")
            String answer,

            @Schema(description = "Les choix d'un QCM, bonnes réponses marquées ; vide pour les autres formats")
            List<ChoiceDto> choices,

            @Schema(
                    description = "Les réponses acceptées par chaque trou d'un code à trous, dans l'ordre des trous ;"
                            + " vide pour les autres formats")
            List<List<String>> blanks,

            @Schema(
                    description = "Les fichiers d'un exercice de code ; absent pour les autres formats",
                    nullable = true)
            @Nullable
            ExerciseDto exercise,

            int version) {
        static CardDto from(CardView card) {
            return new CardDto(
                    card.id(),
                    card.type(),
                    card.prompt(),
                    card.answer(),
                    card.choices().stream().map(ChoiceDto::from).toList(),
                    card.blanks().stream().map(CardBlank::accepted).toList(),
                    card.exercise() == null ? null : ExerciseDto.from(card.exercise()),
                    card.version());
        }
    }

    @Schema(name = "Exercise")
    record ExerciseDto(
            @Schema(description = "Le code de départ, à modifier")
            List<SourceFileDto> starter,

            @Schema(description = "La solution modèle : les mêmes fichiers que le code de départ")
            List<SourceFileDto> solution,

            @Schema(description = "Les tests qui jugent le code")
            List<SourceFileDto> tests) {
        static ExerciseDto from(ExerciseFiles files) {
            return new ExerciseDto(
                    SourceFileDto.from(files.starter()),
                    SourceFileDto.from(files.solution()),
                    SourceFileDto.from(files.tests()));
        }
    }

    @Schema(name = "SourceFile")
    record SourceFileDto(
            @Schema(description = "Chemin relatif, avec des /")
            String path,

            String content) {
        static List<SourceFileDto> from(List<SourceFile> files) {
            return files.stream()
                    .map(file -> new SourceFileDto(file.path(), file.content()))
                    .toList();
        }
    }

    @Schema(name = "CardChoice")
    record ChoiceDto(@Schema(description = "En Markdown") String text, boolean correct) {
        static ChoiceDto from(CardChoice choice) {
            return new ChoiceDto(choice.text(), choice.correct());
        }
    }

    @Schema(name = "CardPage")
    record CardPageDto(List<CardDto> items, int page, int size, long totalItems, int totalPages) {
        static CardPageDto from(Page<CardView> page) {
            return new CardPageDto(
                    page.items().stream().map(CardDto::from).toList(),
                    page.page(),
                    page.size(),
                    page.totalItems(),
                    page.totalPages());
        }
    }

    @Schema(name = "ImportReport")
    record ImportReportDto(
            @Schema(description = "Slug du sujet importé") String subject,
            int themesCreated,
            int themesUpdated,
            int cardsAdded,
            int cardsUpdated,
            int cardsUnchanged,
            int cardsRetired,
            int cardsReactivated,
            List<DuplicateDto> duplicates,
            List<IssueDto> issues) {
        static ImportReportDto from(ImportReport report) {
            return new ImportReportDto(
                    report.subject(),
                    report.themesCreated(),
                    report.themesUpdated(),
                    report.cardsAdded(),
                    report.cardsUpdated(),
                    report.cardsUnchanged(),
                    report.cardsRetired(),
                    report.cardsReactivated(),
                    report.duplicates().stream()
                            .map(duplicate ->
                                    new DuplicateDto(duplicate.themeKey(), duplicate.line(), duplicate.prompt()))
                            .toList(),
                    report.issues().stream()
                            .map(issue -> new IssueDto(issue.line(), issue.reason(), issue.excerpt()))
                            .toList());
        }
    }

    @Schema(name = "ImportDuplicate")
    record DuplicateDto(String themeKey, int line, String prompt) {}

    @Schema(name = "ImportIssue")
    record IssueDto(int line, String reason, String excerpt) {}
}
