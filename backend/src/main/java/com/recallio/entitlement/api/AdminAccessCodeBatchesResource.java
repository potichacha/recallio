package com.recallio.entitlement.api;

import com.recallio.entitlement.application.AccessCodeBatchService;
import com.recallio.entitlement.application.AccessCodeBatchService.BatchDetail;
import com.recallio.entitlement.application.AccessCodeBatchService.CreatedBatch;
import com.recallio.entitlement.domain.AccessCodeBatch;
import com.recallio.entitlement.domain.BatchSummary;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.platform.domain.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Tâche d'administration : les lots de codes d'accès (ADR 0079). Les codes d'un lot ne sont rendus qu'une fois, à sa
 * création ; ensuite, un code ne se reconnaît plus qu'à ses derniers caractères.
 */
@Path("/api/v1/admin/access-code-batches")
@RolesAllowed(AdminAccessCodeBatchesResource.ADMIN_ROLE)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Plus")
@SecurityRequirement(name = "SecurityScheme")
public class AdminAccessCodeBatchesResource {

    static final String ADMIN_ROLE = "admin";
    static final int MAX_PAGE_SIZE = 100;

    private static final int DEFAULT_REDEMPTIONS_PER_CODE = 1;

    private final AccessCodeBatchService batches;
    private final Clock clock;

    public AdminAccessCodeBatchesResource(AccessCodeBatchService batches, Clock clock) {
        this.batches = batches;
        this.clock = clock;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            operationId = "createAccessCodeBatch",
            summary = "Crée un lot de codes d'accès, et rend ses codes cette seule fois")
    @APIResponse(
            responseCode = "201",
            description = "Le lot et ses codes, à noter tout de suite : ils ne seront plus lisibles",
            content = @Content(schema = @Schema(implementation = CreatedBatchDto.class)))
    @APIResponse(responseCode = "403", description = "Rôle admin requis")
    @APIResponse(
            responseCode = "422",
            description = "La date d'expiration est déjà passée",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public Response createAccessCodeBatch(@Valid @NotNull NewBatchDto request) {
        Integer redemptions = request.maxRedemptionsPerCode();
        CreatedBatch created = batches.create(
                request.label(),
                request.codeCount(),
                request.plusDays(),
                redemptions == null ? DEFAULT_REDEMPTIONS_PER_CODE : redemptions,
                request.expiresAt());
        return Response.status(Response.Status.CREATED)
                .entity(new CreatedBatchDto(BatchDto.from(created.summary(), clock.instant()), created.codes()))
                .build();
    }

    @GET
    @Operation(operationId = "listAccessCodeBatches", summary = "Les lots de codes d'accès, le plus récent d'abord")
    @APIResponse(responseCode = "200", description = "Une page de lots, avec ce que leurs codes sont devenus")
    @APIResponse(responseCode = "403", description = "Rôle admin requis")
    public BatchPageDto listAccessCodeBatches(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return BatchPageDto.from(batches.list(page, size), clock.instant());
    }

    @GET
    @Path("/{id}")
    @Operation(
            operationId = "getAccessCodeBatch",
            summary = "Un lot et ses codes, reconnus à leurs derniers caractères")
    @APIResponse(responseCode = "200", description = "Le lot et ses codes")
    @APIResponse(responseCode = "403", description = "Rôle admin requis")
    @APIResponse(responseCode = "404", description = "Aucun lot de cet identifiant")
    public BatchDetailDto getAccessCodeBatch(@PathParam("id") UUID id) {
        return BatchDetailDto.from(batches.detail(id), clock.instant());
    }

    @PUT
    @Path("/{id}/revocation")
    @Operation(
            operationId = "revokeAccessCodeBatch",
            summary = "Révoque un lot : ses codes ne marchent plus, les droits déjà ouverts restent")
    @APIResponse(responseCode = "200", description = "Le lot révoqué ; inchangé s'il l'était déjà")
    @APIResponse(responseCode = "403", description = "Rôle admin requis")
    @APIResponse(responseCode = "404", description = "Aucun lot de cet identifiant")
    public BatchDto revokeAccessCodeBatch(@PathParam("id") UUID id) {
        return BatchDto.from(batches.revoke(id), clock.instant());
    }

    @Schema(
            name = "NewAccessCodeBatch",
            requiredProperties = {"label", "codeCount", "plusDays"})
    public record NewBatchDto(
            @Schema(description = "Ce qui fait reconnaître le lot")
            @NotBlank
            @Size(max = AccessCodeBatch.MAX_LABEL_LENGTH)
            String label,

            @Schema(description = "Le nombre de codes à créer") @Min(1) @Max(AccessCodeBatch.MAX_CODES)
            int codeCount,

            @Schema(description = "Les jours de Plus que donne chaque utilisation d'un code")
            @Min(1)
            @Max(AccessCodeBatch.MAX_PLUS_DAYS)
            int plusDays,

            @Schema(
                    description = "Le nombre de comptes qui peuvent utiliser un même code ; 1 par défaut",
                    defaultValue = "1",
                    nullable = true)
            @Nullable
            @Min(1)
            @Max(AccessCodeBatch.MAX_REDEMPTIONS_PER_CODE)
            Integer maxRedemptionsPerCode,

            @Schema(description = "Quand les codes cessent de marcher ; absente s'ils n'expirent pas", nullable = true)
            @Nullable
            Instant expiresAt) {}

    @Schema(name = "AccessCodeBatch")
    public record BatchDto(
            UUID id,
            String label,

            @Schema(description = "Les jours de Plus que donne chaque utilisation d'un code")
            int plusDays,

            @Schema(description = "Le nombre de comptes qui peuvent utiliser un même code")
            int maxRedemptionsPerCode,

            @Schema(description = "Le nombre de codes créés")
            int codeCount,

            @Schema(description = "Les codes qu'au moins un compte a utilisés")
            int usedCodes,

            @Schema(description = "Le nombre total d'utilisations")
            int redemptions,

            Instant createdAt,

            @Schema(description = "Quand les codes cessent de marcher ; absente s'ils n'expirent pas", nullable = true)
            @Nullable
            Instant expiresAt,

            @Schema(description = "Vrai si cette date est passée")
            boolean expired,

            @Schema(description = "Quand le lot a été révoqué ; absente s'il ne l'est pas", nullable = true) @Nullable
            Instant revokedAt) {

        static BatchDto from(BatchSummary summary, Instant now) {
            AccessCodeBatch batch = summary.batch();
            return new BatchDto(
                    batch.id(),
                    batch.label(),
                    batch.plusDays(),
                    batch.maxRedemptionsPerCode(),
                    summary.codes(),
                    summary.usedCodes(),
                    summary.redemptions(),
                    batch.createdAt(),
                    batch.expiresAt(),
                    batch.isExpiredAt(now),
                    batch.revokedAt());
        }
    }

    @Schema(name = "CreatedAccessCodeBatch")
    public record CreatedBatchDto(
            BatchDto batch,

            @Schema(description = "Les codes, lisibles cette seule fois : le serveur n'en garde que l'empreinte")
            List<String> codes) {

        /** Les codes ne s'écrivent ni dans un message d'erreur ni dans un log. */
        @Override
        public String toString() {
            return "CreatedBatchDto[" + batch.id() + ", ***]";
        }
    }

    @Schema(name = "AccessCodeBatchPage")
    public record BatchPageDto(List<BatchDto> items, int page, int size, long totalItems, int totalPages) {

        static BatchPageDto from(Page<BatchSummary> page, Instant now) {
            return new BatchPageDto(
                    page.items().stream()
                            .map(summary -> BatchDto.from(summary, now))
                            .toList(),
                    page.page(),
                    page.size(),
                    page.totalItems(),
                    page.totalPages());
        }
    }

    @Schema(name = "AccessCodeBatchDetail")
    public record BatchDetailDto(
            BatchDto batch,

            @Schema(description = "Les codes du lot, dans l'ordre de leur création")
            List<CodeDto> codes) {

        static BatchDetailDto from(BatchDetail detail, Instant now) {
            return new BatchDetailDto(
                    BatchDto.from(detail.summary(), now),
                    detail.codes().stream().map(CodeDto::from).toList());
        }
    }

    @Schema(name = "AccessCodeSummary")
    public record CodeDto(
            @Schema(description = "Les derniers caractères du code : le code lui-même n'est pas gardé")
            String suffix,

            @Schema(description = "Le nombre de comptes qui l'ont utilisé")
            int redemptions) {

        static CodeDto from(BatchSummary.Code code) {
            return new CodeDto(code.suffix(), code.redemptions());
        }
    }
}
