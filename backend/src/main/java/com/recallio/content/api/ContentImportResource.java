package com.recallio.content.api;

import com.recallio.content.api.ContentDtos.ImportReportDto;
import com.recallio.content.application.ContentImportService;
import com.recallio.platform.api.ProblemDetail;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Tâche d'administration : (ré)importer le document de référence d'un sujet. Le sujet est décrit par l'en-tête du
 * document ; les autres sujets ne sont pas touchés. Relancer l'import ne crée aucun doublon.
 */
@Path("/api/v1/admin/content-imports")
@RolesAllowed(ContentImportResource.ADMIN_ROLE)
@Tag(name = "Administration")
@SecurityRequirement(name = "SecurityScheme")
public class ContentImportResource {

    static final String ADMIN_ROLE = "admin";

    private final ContentImportService importService;

    public ContentImportResource(ContentImportService importService) {
        this.importService = importService;
    }

    @POST
    @Consumes({"text/markdown", MediaType.TEXT_PLAIN})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(operationId = "importContent", summary = "Importe le document de référence Markdown d'un sujet")
    @RequestBody(
            description = "Le contenu complet d'un fichier <sujet>-entretien-reference.md, en-tête compris",
            required = true)
    @APIResponse(responseCode = "200", description = "Rapport d'import")
    @APIResponse(
            responseCode = "422",
            description = "En-tête du document absent ou invalide",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "401", description = "Non authentifié")
    @APIResponse(responseCode = "403", description = "Rôle admin requis")
    public ImportReportDto importContent(String markdown) {
        return ImportReportDto.from(importService.importContent(markdown));
    }
}
