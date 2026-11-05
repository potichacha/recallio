package com.recallio.ranked.api;

import com.recallio.content.SubjectDirectory;
import com.recallio.ranked.api.RankedDtos.DuelSummaryDto;
import com.recallio.ranked.api.RankedDtos.OverviewDto;
import com.recallio.ranked.application.RankedViews;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Le classé de l'utilisateur connecté (ADR 0046) : son rang par sujet, ses insignes, ses derniers duels. */
@Path("/api/v1/me/ranked")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Classé")
@SecurityRequirement(name = "SecurityScheme")
public class MyRankedResource {

    private final RankedViews views;
    private final SubjectDirectory subjects;
    private final CurrentUser currentUser;

    public MyRankedResource(RankedViews views, SubjectDirectory subjects, CurrentUser currentUser) {
        this.views = views;
        this.subjects = subjects;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getMyRanked", summary = "Le rang de chaque sujet suivi, le placement et les insignes")
    @APIResponse(responseCode = "200", description = "La saison en cours, sujet par sujet")
    public OverviewDto getMyRanked() {
        return OverviewDto.from(views.overview(currentUser.get()), subjects.publishedTitles());
    }

    @GET
    @Path("/duels")
    @Operation(operationId = "listMyDuels", summary = "Les derniers duels, les plus récents d'abord")
    @APIResponse(responseCode = "200", description = "Les duels, réglés ou non")
    public List<DuelSummaryDto> listMyDuels() {
        return views.recent(currentUser.get()).stream()
                .map(DuelSummaryDto::from)
                .toList();
    }
}
