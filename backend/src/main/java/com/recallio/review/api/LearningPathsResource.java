package com.recallio.review.api;

import com.recallio.content.CardType;
import com.recallio.content.NotionInfo;
import com.recallio.platform.api.ProblemDetail;
import com.recallio.review.application.LearningPathService;
import com.recallio.review.application.LearningPathService.NotionStep;
import com.recallio.review.application.LearningPathService.SubjectPath;
import com.recallio.review.domain.track.LearningPath.NotionStatus;
import com.recallio.review.domain.track.MasteryLevel;
import com.recallio.review.domain.track.NotionMastery;
import com.recallio.review.domain.track.NotionMastery.NextLevel;
import com.recallio.user.CurrentUser;
import com.recallio.user.LearningTrack;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/** La carte du parcours d'un sujet (ADR 0032) : chacun ne voit que sa propre progression (CLAUDE.md, 4.5). */
@Path("/api/v1/learning-paths")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Révision")
@SecurityRequirement(name = "SecurityScheme")
public class LearningPathsResource {

    private final LearningPathService paths;
    private final CurrentUser currentUser;

    public LearningPathsResource(LearningPathService paths, CurrentUser currentUser) {
        this.paths = paths;
        this.currentUser = currentUser;
    }

    @GET
    @Path("/{subject}")
    @Operation(
            operationId = "getLearningPath",
            summary = "Le parcours d'un sujet : ses notions par chapitre, chacune avec son état pour l'utilisateur")
    @APIResponse(responseCode = "200", description = "Le parcours")
    @APIResponse(
            responseCode = "404",
            description = "Sujet inconnu ou non publié",
            content =
                    @Content(
                            mediaType = ProblemDetail.MEDIA_TYPE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    public LearningPathDto getLearningPath(
            @Parameter(description = "Slug du sujet") @PathParam("subject") String subject) {
        return LearningPathDto.from(paths.path(currentUser.get(), subject));
    }

    @Schema(name = "LearningPath")
    record LearningPathDto(
            String subjectSlug,

            @Schema(
                    description =
                            "Le parcours de l'utilisateur pour ce sujet ; le parcours par défaut s'il ne le suit pas")
            LearningTrack track,

            @Schema(description = "Les chapitres du référentiel, dans l'ordre ; vide pour un sujet sans référentiel")
            List<PathChapterDto> chapters) {

        static LearningPathDto from(SubjectPath path) {
            Map<String, PathChapterDto> chapters = new LinkedHashMap<>();
            for (NotionStep step : path.notions()) {
                NotionInfo notion = step.notion();
                chapters.computeIfAbsent(
                                notion.chapter(),
                                _ -> new PathChapterDto(notion.chapter(), notion.chapterTitle(), new ArrayList<>()))
                        .notions()
                        .add(PathNotionDto.from(step));
            }
            return new LearningPathDto(path.subjectSlug(), path.track(), List.copyOf(chapters.values()));
        }
    }

    @Schema(name = "PathChapter")
    record PathChapterDto(String id, String title, List<PathNotionDto> notions) {}

    @Schema(name = "PathNotion")
    record PathNotionDto(
            String slug,
            String title,
            NotionInfo.Level level,

            @Schema(description = "1 essentiel, 2 courant, 3 avancé, 4 rare")
            int priority,

            NotionStatus status,

            @Schema(description = "Les cartes de la notion") int cardCount,

            @Schema(description = "Les cartes de la notion déjà vues")
            int seenCount,

            @Schema(description = "Le niveau de maîtrise de la notion (ADR 0034)")
            MasteryLevel mastery,

            @Schema(description = "Ce qui mène au niveau suivant ; absent au niveau le plus haut", nullable = true)
            @Nullable
            NextLevelDto nextLevel) {

        static PathNotionDto from(NotionStep step) {
            NotionInfo notion = step.notion();
            NotionMastery mastery = step.mastery();
            return new PathNotionDto(
                    notion.slug(),
                    notion.title(),
                    notion.level(),
                    notion.priority(),
                    step.status(),
                    notion.cardCount(),
                    mastery.seen(),
                    mastery.level(),
                    mastery.nextLevel().map(NextLevelDto::from).orElse(null));
        }
    }

    @Schema(
            name = "NextMasteryLevel",
            description = "Ce que demande le niveau de maîtrise suivant, et où en est l'utilisateur")
    record NextLevelDto(
            MasteryLevel level,

            @Schema(
                    description = "Les cartes qui comptent déjà : vues pour « découverte », réussies pour « acquise »,"
                            + " maîtrisées pour « maîtrisée », retenues sur la durée pour « experte »")
            int cards,

            @Schema(description = "Les cartes que ce niveau demande")
            int cardsNeeded,

            @Schema(description = "Les formats différents parmi ces cartes ; 0 si le niveau n'en demande pas")
            int formats,

            @Schema(description = "Les formats différents que ce niveau demande ; 0 s'il n'en demande pas")
            int formatsNeeded,

            @Schema(description = "Les formats de la notion encore jamais réussis, que le niveau « experte » demande")
            List<CardType> missingFormats) {

        static NextLevelDto from(NextLevel next) {
            return new NextLevelDto(
                    next.level(),
                    next.cards(),
                    next.cardsNeeded(),
                    next.formats(),
                    next.formatsNeeded(),
                    next.missingFormats().stream().sorted().toList());
        }
    }
}
