package com.recallio.content.api;

import com.recallio.content.TourPage;
import com.recallio.content.api.ContentDtos.SourceFileDto;
import com.recallio.content.application.ContentBrowsingService;
import com.recallio.content.domain.catalog.ContentCatalog.TourEntry;
import com.recallio.content.domain.catalog.ContentCatalog.TourExercise;
import com.recallio.content.domain.catalog.ContentCatalog.TourNotionPages;
import com.recallio.content.domain.catalog.TourOutline;
import com.recallio.platform.api.ProblemDetail;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le Tour d'un sujet (ADR 0037) : son sommaire, puis les pages de chaque notion. Public, comme le catalogue ; les sujets
 * en brouillon ne sont visibles que des admins. La solution et la sortie attendue d'un « À toi » ne quittent pas le
 * serveur.
 */
@Path("/api/v1/subjects/{subject}/tour")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tour", description = "Apprendre un langage pas à pas, page par page (ADR 0037)")
public class TourResource {

    private final ContentBrowsingService browsing;
    private final CatalogVisibility visibility;

    public TourResource(ContentBrowsingService browsing, CatalogVisibility visibility) {
        this.browsing = browsing;
        this.visibility = visibility;
    }

    @GET
    @Operation(
            operationId = "getTour",
            summary = "Le sommaire du Tour d'un sujet : ses domaines, ses chapitres et ses notions, dans l'ordre")
    @APIResponse(responseCode = "200", description = "Le sommaire ; sans domaine si le sujet n'a pas encore de Tour")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public TourOutlineDto getTour(@PathParam("subject") String subject) {
        return TourOutlineDto.from(browsing.tour(subject, visibility.current()));
    }

    @GET
    @Path("/{notion}")
    @Operation(operationId = "getTourNotion", summary = "Les pages d'une notion du Tour, dans l'ordre")
    @APIResponse(responseCode = "200", description = "Les pages")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu, ou notion qui n'est pas dans son Tour",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public TourNotionDto getTourNotion(@PathParam("subject") String subject, @PathParam("notion") String notion) {
        return TourNotionDto.from(browsing.tourNotion(subject, notion, visibility.current()));
    }

    @Schema(name = "TourOutline")
    public record TourOutlineDto(
            String subjectSlug,
            String subjectTitle,

            @Schema(description = "Dans l'ordre du référentiel ; vide si le sujet n'a pas encore de Tour")
            List<TourDomainDto> domains) {

        static TourOutlineDto from(TourOutline outline) {
            return new TourOutlineDto(
                    outline.subjectSlug(),
                    outline.subjectTitle(),
                    outline.domains().stream().map(TourDomainDto::from).toList());
        }
    }

    @Schema(name = "TourDomain")
    public record TourDomainDto(String id, String title, List<TourChapterDto> chapters) {

        static TourDomainDto from(TourOutline.Domain domain) {
            return new TourDomainDto(
                    domain.id(),
                    domain.title(),
                    domain.chapters().stream().map(TourChapterDto::from).toList());
        }
    }

    @Schema(name = "TourChapter")
    public record TourChapterDto(
            String id,
            String title,
            List<TourEntryDto> notions,

            @Schema(description = "Les exercices de code du chapitre, à faire à la fin de ses pages")
            List<TourExerciseDto> exercises) {

        static TourChapterDto from(TourOutline.Chapter chapter) {
            return new TourChapterDto(
                    chapter.id(),
                    chapter.title(),
                    chapter.notions().stream().map(TourEntryDto::from).toList(),
                    chapter.exercises().stream().map(TourExerciseDto::from).toList());
        }
    }

    @Schema(name = "TourExercise")
    public record TourExerciseDto(
            @Schema(description = "La notion de l'exercice") String notion,

            UUID cardId,

            @Schema(description = "La première ligne de son énoncé, en Markdown")
            String summary) {

        static TourExerciseDto from(TourExercise exercise) {
            return new TourExerciseDto(exercise.notionSlug(), exercise.cardId(), exercise.summary());
        }
    }

    @Schema(name = "TourEntry")
    public record TourEntryDto(
            String slug,

            @Schema(description = "Le titre de sa première page")
            String title,

            int pageCount) {

        static TourEntryDto from(TourEntry entry) {
            return new TourEntryDto(entry.slug(), entry.title(), entry.pageCount());
        }
    }

    @Schema(name = "TourNotion")
    public record TourNotionDto(String slug, String title, String chapterTitle, List<TourPageDto> pages) {

        static TourNotionDto from(TourNotionPages notion) {
            return new TourNotionDto(
                    notion.slug(),
                    notion.title(),
                    notion.chapterTitle(),
                    notion.pages().stream()
                            .map(page -> TourPageDto.from(page, notion.title()))
                            .toList());
        }
    }

    @Schema(name = "TourPage")
    public record TourPageDto(
            @Schema(description = "Son titre, ou celui de la notion")
            String title,

            @Schema(description = "En Markdown") String text,

            @Schema(description = "Le programme de la page, à modifier et à exécuter")
            List<SourceFileDto> example,

            @Schema(description = "Un « À toi » ; la sortie attendue reste sur le serveur", nullable = true) @Nullable
            TourTaskDto task) {

        static TourPageDto from(TourPage page, String notionTitle) {
            TourPage.Task task = page.task();
            String title = page.title();
            return new TourPageDto(
                    title == null ? notionTitle : title,
                    page.text(),
                    SourceFileDto.from(page.example().files()),
                    task == null ? null : new TourTaskDto(task.instructions(), SourceFileDto.from(task.starter())));
        }
    }

    @Schema(name = "TourTask")
    public record TourTaskDto(
            @Schema(description = "La consigne, en Markdown")
            String instructions,

            @Schema(description = "Le code de départ, à modifier jusqu'à obtenir la sortie attendue")
            List<SourceFileDto> starter) {}
}
