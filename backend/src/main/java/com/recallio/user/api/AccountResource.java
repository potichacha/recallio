package com.recallio.user.api;

import com.recallio.user.CurrentUser;
import com.recallio.user.application.AccountService;
import com.recallio.user.application.AccountService.AccountExport;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le compte de l'utilisateur connecté, au sens du RGPD (ADR 0056) : il récupère ses données et les fait effacer. Chacun
 * n'agit que sur son propre compte : la route ne prend aucun identifiant.
 */
@Path("/api/v1/me/account")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Profil")
@SecurityRequirement(name = "SecurityScheme")
public class AccountResource {

    static final String EXPORT_FILE = "attachment; filename=\"recallio-export.json\"";

    private final AccountService accounts;
    private final CurrentUser currentUser;

    public AccountResource(AccountService accounts, CurrentUser currentUser) {
        this.accounts = accounts;
        this.currentUser = currentUser;
    }

    /** La seule route sans pagination : un export se télécharge en entier, une fois. */
    @GET
    @Path("/export")
    @Operation(
            operationId = "exportAccount",
            summary = "Toutes les données de l'utilisateur, dans un fichier JSON à télécharger")
    @APIResponse(
            responseCode = "200",
            description = "Les données, par module puis par table",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AccountExportDto.class)))
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public Response exportAccount() {
        AccountExport export = accounts.export(currentUser.get());
        return Response.ok(new AccountExportDto(export.exportedAt(), export.sections()))
                .header(HttpHeaders.CONTENT_DISPOSITION, EXPORT_FILE)
                .build();
    }

    @DELETE
    @Operation(
            operationId = "deleteAccount",
            summary = "Supprime le compte et ses données, tout de suite et définitivement")
    @APIResponse(responseCode = "204", description = "Le compte est supprimé")
    @APIResponse(responseCode = "401", description = "Non authentifié")
    public void deleteAccount() {
        accounts.delete(currentUser.get());
    }

    @Schema(name = "AccountExport")
    public record AccountExportDto(
            Instant exportedAt,

            @Schema(description = "Par module, puis par table, les lignes de l'utilisateur telles qu'enregistrées")
            Map<String, Map<String, List<Map<String, @Nullable Object>>>> sections) {}
}
