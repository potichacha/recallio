package com.recallio.gamification.api;

import com.recallio.gamification.application.AchievementService.AchievementView;
import com.recallio.gamification.application.PublicSkillProfileService;
import com.recallio.gamification.application.PublicSkillProfileService.PublicSkillProfile;
import com.recallio.gamification.application.PublicSkillProfileService.SubjectProfile;
import com.recallio.gamification.domain.achievement.Achievement;
import com.recallio.ranked.RankedProfiles.PublicRank;
import com.recallio.review.SkillProfiles.Declared;
import com.recallio.review.SkillProfiles.Verified;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;

/**
 * Le profil public de compétences (ADR 0043), lisible sans compte, comme le catalogue (voir les autorisations HTTP de
 * application.properties). Un profil privé répond 404, comme une adresse inconnue.
 */
@Path("/api/v1/public-profiles")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Profil public")
public class PublicProfilesResource {

    static final int MAX_HANDLE_LENGTH = 30;

    private final PublicSkillProfileService profiles;

    public PublicProfilesResource(PublicSkillProfileService profiles) {
        this.profiles = profiles;
    }

    @GET
    @Path("/{handle}")
    @Operation(operationId = "getPublicProfile", summary = "Le profil public de compétences d'un utilisateur")
    @APIResponse(responseCode = "200", description = "Le profil")
    @APIResponse(responseCode = "404", description = "Aucun profil public à cette adresse")
    public PublicProfileDto getPublicProfile(@PathParam("handle") @Size(max = MAX_HANDLE_LENGTH) String handle) {
        return PublicProfileDto.from(profiles.profile(handle));
    }

    @Schema(name = "PublicProfile")
    public record PublicProfileDto(
            String handle,
            String displayName,

            @Schema(description = "Jours consécutifs avec l'objectif du jour atteint")
            int streak,

            @Schema(description = "Les sujets suivis, dans l'ordre du catalogue")
            List<PublicSubjectDto> subjects,

            @Schema(description = "Les succès débloqués") List<PublicAchievementDto> achievements) {

        static PublicProfileDto from(PublicSkillProfile profile) {
            return new PublicProfileDto(
                    profile.handle(),
                    profile.displayName(),
                    profile.streak(),
                    profile.subjects().stream().map(PublicSubjectDto::from).toList(),
                    profile.achievements().stream()
                            .map(PublicAchievementDto::from)
                            .toList());
        }
    }

    @Schema(name = "PublicSubject")
    public record PublicSubjectDto(
            String slug,
            String title,
            int level,
            long xp,

            @Schema(description = "Ce que le serveur a vérifié")
            VerifiedSkillsDto verified,

            @Schema(description = "Le rang en classé, vérifié ; absent pendant le placement, ou sans duel") @Nullable
            PublicRankDto rank,

            @Schema(description = "Ce que l'utilisateur déclare en révision personnelle, auto-évaluée en partie")
            DeclaredSkillsDto declared) {

        static PublicSubjectDto from(SubjectProfile subject) {
            return new PublicSubjectDto(
                    subject.skills().subjectSlug(),
                    subject.title(),
                    subject.level().level(),
                    subject.level().xp(),
                    VerifiedSkillsDto.from(subject.skills().verified()),
                    PublicRankDto.from(subject.rank()),
                    DeclaredSkillsDto.from(subject.skills().declared()));
        }
    }

    @Schema(name = "PublicRank", description = "Un rang ; la division va de 4 (la plus basse) à 1, 0 pour MASTER")
    public record PublicRankDto(String tier, int division) {

        static @Nullable PublicRankDto from(@Nullable PublicRank rank) {
            return rank == null ? null : new PublicRankDto(rank.tier(), rank.division());
        }
    }

    @Schema(name = "VerifiedSkills")
    public record VerifiedSkillsDto(
            @Schema(description = "Exercices de code réussis, jugés par leurs tests")
            int exercisesPassed,

            @Schema(description = "QCM, vrai ou faux et codes à compléter réussis, corrigés par le serveur")
            int correctAnswers,

            @Schema(description = "Notions du Tour validées")
            int tourNotionsValidated) {

        static VerifiedSkillsDto from(Verified verified) {
            return new VerifiedSkillsDto(
                    verified.exercisesPassed(), verified.correctAnswers(), verified.tourNotionsValidated());
        }
    }

    @Schema(name = "DeclaredSkills")
    public record DeclaredSkillsDto(
            @Schema(description = "Les notions du sujet ; aucune sans référentiel")
            int notions,

            int acquired,
            int mastered,
            int expert,

            @Schema(description = "L'indice de préparation, de 0 à 100 (ADR 0041)")
            int readinessPercent) {

        static DeclaredSkillsDto from(Declared declared) {
            return new DeclaredSkillsDto(
                    declared.notions(),
                    declared.acquired(),
                    declared.mastered(),
                    declared.expert(),
                    declared.readinessPercent());
        }
    }

    @Schema(name = "PublicAchievement")
    public record PublicAchievementDto(Achievement achievement, Instant unlockedAt) {

        static PublicAchievementDto from(AchievementView view) {
            Instant unlockedAt = view.unlockedAt();
            if (unlockedAt == null) {
                throw new IllegalArgumentException("Seuls les succès débloqués sont publiés : " + view.achievement());
            }
            return new PublicAchievementDto(view.achievement(), unlockedAt);
        }
    }
}
