package com.recallio.content.api;

import com.recallio.content.api.ContentDtos.CardPageDto;
import com.recallio.content.api.ContentDtos.ThemeDto;
import com.recallio.content.application.ContentBrowsingService;
import com.recallio.platform.api.ProblemDetail;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Un thème et ses cartes, par son slug (unique entre sujets, car préfixé par le sujet). La liste des thèmes se lit par
 * sujet : {@link SubjectsResource}.
 */
@Path("/api/v1/themes")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Contenu", description = "Sujets, thèmes et cartes publiés")
public class ThemesResource {

    static final int MAX_PAGE_SIZE = 100;

    private final ContentBrowsingService browsing;
    private final CatalogVisibility visibility;

    public ThemesResource(ContentBrowsingService browsing, CatalogVisibility visibility) {
        this.browsing = browsing;
        this.visibility = visibility;
    }

    @GET
    @Path("/{slug}")
    @Operation(operationId = "getTheme", summary = "Détail d'un thème")
    @APIResponse(responseCode = "200", description = "Le thème")
    @APIResponse(
            responseCode = "404",
            description = "Thème inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public ThemeDto getTheme(@PathParam("slug") String slug) {
        return ThemeDto.from(browsing.theme(slug, visibility.current()));
    }

    @GET
    @Path("/{slug}/cards")
    @Operation(operationId = "listThemeCards", summary = "Cartes actives d'un thème, paginées")
    @APIResponse(responseCode = "200", description = "Une page de cartes")
    @APIResponse(
            responseCode = "400",
            description = "Pagination invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(
            responseCode = "404",
            description = "Thème inconnu",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public CardPageDto listThemeCards(
            @PathParam("slug") String slug,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return CardPageDto.from(browsing.cards(slug, visibility.current(), page, size));
    }
}
