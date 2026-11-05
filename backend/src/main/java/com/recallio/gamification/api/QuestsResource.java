package com.recallio.gamification.api;

import com.recallio.gamification.application.QuestService;
import com.recallio.gamification.application.QuestService.Board;
import com.recallio.gamification.application.QuestService.QuestView;
import com.recallio.gamification.domain.quest.Quest;
import com.recallio.user.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDate;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Les quêtes du jour et de la semaine de l'utilisateur connecté (ADR 0040). */
@Path("/api/v1/me/quests")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Progression")
@SecurityRequirement(name = "SecurityScheme")
public class QuestsResource {

    private final QuestService quests;
    private final CurrentUser currentUser;

    public QuestsResource(QuestService quests, CurrentUser currentUser) {
        this.quests = quests;
        this.currentUser = currentUser;
    }

    @GET
    @Operation(operationId = "getMyQuests", summary = "Les quêtes du jour et de la semaine, et où en est l'utilisateur")
    @APIResponse(responseCode = "200", description = "Trois quêtes du jour, deux de la semaine")
    public QuestBoardDto getMyQuests() {
        return QuestBoardDto.from(quests.board(currentUser.get()));
    }

    @Schema(name = "QuestBoard")
    public record QuestBoardDto(
            List<QuestProgressDto> daily,

            @Schema(description = "Le jour où les quêtes du jour changent, dans le fuseau de l'utilisateur")
            LocalDate dailyEndsOn,

            List<QuestProgressDto> weekly,

            @Schema(description = "Le lundi où les quêtes de la semaine changent")
            LocalDate weeklyEndsOn) {

        static QuestBoardDto from(Board board) {
            return new QuestBoardDto(
                    board.daily().stream().map(QuestProgressDto::from).toList(),
                    board.dailyEndsOn(),
                    board.weekly().stream().map(QuestProgressDto::from).toList(),
                    board.weeklyEndsOn());
        }
    }

    @Schema(name = "QuestProgress")
    public record QuestProgressDto(
            Quest quest,

            @Schema(description = "Ce qu'il faut atteindre") int target,

            @Schema(description = "Où en est l'utilisateur, plafonné à l'objectif")
            int progress,

            @Schema(description = "L'XP que rapporte la quête, une fois terminée")
            int xp,

            boolean completed) {

        static QuestProgressDto from(QuestView view) {
            return new QuestProgressDto(
                    view.quest(),
                    view.quest().target(),
                    view.progress(),
                    view.quest().xp(),
                    view.completed());
        }
    }
}
