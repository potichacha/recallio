package com.recallio.content.api;

import com.recallio.content.api.ContentDtos.SubjectDto;
import com.recallio.content.api.ContentDtos.ThemeDto;
import com.recallio.content.application.ContentBrowsingService;
import com.recallio.platform.api.ProblemDetail;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Catalogue public des sujets (CLAUDE.md, sections 2.5 et 4.5) ; les brouillons ne sont visibles que des admins. */
@Path("/api/v1/subjects")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Contenu", description = "Sujets, thèmes et cartes publiés")
public class SubjectsResource {

    private final ContentBrowsingService browsing;
    private final CatalogVisibility visibility;

    public SubjectsResource(ContentBrowsingService browsing, CatalogVisibility visibility) {
        this.browsing = browsing;
        this.visibility = visibility;
    }

    @GET
    @Operation(operationId = "listSubjects", summary = "Liste les sujets du catalogue, dans l'ordre d'affichage")
    public List<SubjectDto> listSubjects() {
        return browsing.subjects(visibility.current()).stream()
                .map(SubjectDto::from)
                .toList();
    }

    @GET
    @Path("/{slug}")
    @Operation(operationId = "getSubject", summary = "Détail d'un sujet")
    @APIResponse(responseCode = "200", description = "Le sujet")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public SubjectDto getSubject(@PathParam("slug") String slug) {
        return SubjectDto.from(browsing.subject(slug, visibility.current()));
    }

    @GET
    @Path("/{slug}/themes")
    @Operation(operationId = "listSubjectThemes", summary = "Thèmes d'un sujet, dans l'ordre du document")
    @APIResponse(responseCode = "200", description = "Les thèmes du sujet")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public List<ThemeDto> listSubjectThemes(@PathParam("slug") String slug) {
        return browsing.themes(slug, visibility.current()).stream()
                .map(ThemeDto::from)
                .toList();
    }
}
