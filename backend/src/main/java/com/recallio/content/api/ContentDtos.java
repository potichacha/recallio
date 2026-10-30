package com.recallio.content.api;

import com.recallio.content.CardType;
import com.recallio.content.domain.catalog.ContentCatalog.CardView;
import com.recallio.content.domain.catalog.ContentCatalog.ThemeSummary;
import com.recallio.content.domain.importing.ImportReport;
import com.recallio.platform.domain.Page;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Contrat JSON du module content : le domaine n'est jamais exposé directement. */
final class ContentDtos {

    private ContentDtos() {}

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
            @Schema(description = "Verso, en Markdown") String answer,
            int version) {
        static CardDto from(CardView card) {
            return new CardDto(card.id(), card.type(), card.prompt(), card.answer(), card.version());
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
