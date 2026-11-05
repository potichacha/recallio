package com.recallio.ranked.api;

import com.recallio.content.CardChoice;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.CardType;
import com.recallio.ranked.application.DuelService.Answered;
import com.recallio.ranked.application.DuelService.Turn;
import com.recallio.ranked.application.RankedViews.DuelView;
import com.recallio.ranked.application.RankedViews.Overview;
import com.recallio.ranked.application.RankedViews.Result;
import com.recallio.ranked.domain.Duel;
import com.recallio.ranked.domain.Duel.DuelPlayer;
import com.recallio.ranked.domain.Duel.Mode;
import com.recallio.ranked.domain.Duel.Status;
import com.recallio.ranked.domain.DuelGrading.BlankResult;
import com.recallio.ranked.domain.Rank;
import com.recallio.ranked.domain.Rank.Tier;
import com.recallio.ranked.domain.RankedRepositories.Badge;
import com.recallio.ranked.domain.Standing;
import com.recallio.ranked.domain.glicko.Glicko2.Game;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Les DTO du classé (ADR 0046) : des rangs, jamais de cote. */
final class RankedDtos {

    private RankedDtos() {}

    enum Outcome {
        WIN,
        LOSS,
        DRAW;

        static Outcome of(double score) {
            if (score == Game.WIN) {
                return WIN;
            }
            return score == Game.LOSS ? LOSS : DRAW;
        }
    }

    @Schema(name = "Rank", description = "Un rang ; la division va de 4 (la plus basse) à 1, 0 pour Maître")
    record RankDto(Tier tier, int division) {

        static @Nullable RankDto from(@Nullable Rank rank) {
            return rank == null ? null : new RankDto(rank.tier(), rank.division());
        }
    }

    @Schema(name = "RankedOverview")
    record OverviewDto(
            @Schema(description = "La saison en cours, par exemple 2026-T4")
            String season,

            @Schema(description = "Le jour où elle se termine")
            LocalDate seasonEndsOn,

            List<SubjectRankDto> subjects,

            @Schema(description = "Le meilleur rang de chaque saison finie")
            List<BadgeDto> badges) {

        static OverviewDto from(Overview overview, Map<String, String> titles) {
            return new OverviewDto(
                    overview.season().id(),
                    overview.season().endsOn(),
                    overview.standings().stream()
                            .map(standing -> SubjectRankDto.from(standing, titles))
                            .toList(),
                    overview.badges().stream()
                            .map(badge -> BadgeDto.from(badge, titles))
                            .toList());
        }
    }

    @Schema(name = "SubjectRank")
    record SubjectRankDto(
            String subject,
            String title,

            @Schema(description = "Vrai pendant les matchs de placement : le rang reste caché")
            boolean placing,

            @Schema(description = "Les duels joués cette saison")
            int seasonDuels,

            @Schema(description = "Les duels de placement d'une saison")
            int placementDuels,

            @Schema(description = "Absent pendant le placement") @Nullable
            RankDto rank,

            int wins,
            int losses,
            int draws) {

        static SubjectRankDto from(Standing standing, Map<String, String> titles) {
            return new SubjectRankDto(
                    standing.subjectSlug(),
                    titles.getOrDefault(standing.subjectSlug(), standing.subjectSlug()),
                    standing.isPlacing(),
                    standing.seasonDuels(),
                    Standing.PLACEMENT_DUELS,
                    RankDto.from(standing.rank().orElse(null)),
                    standing.wins(),
                    standing.losses(),
                    standing.draws());
        }
    }

    @Schema(name = "SeasonBadge")
    record BadgeDto(String subject, String title, String season, Tier tier) {

        static BadgeDto from(Badge badge, Map<String, String> titles) {
            return new BadgeDto(
                    badge.subjectSlug(),
                    titles.getOrDefault(badge.subjectSlug(), badge.subjectSlug()),
                    badge.season().id(),
                    badge.tier());
        }
    }

    @Schema(name = "Duel")
    record DuelDto(
            UUID id,
            String subject,
            Mode mode,
            Status status,
            int questionCount,

            @Schema(
                    description =
                            "Ouvert, le duel attend un adversaire jusque-là ; complet, chacun doit avoir fini avant")
            Instant expiresAt,

            DuelSideDto me,

            @Schema(description = "Absent tant que personne n'a rejoint le duel") @Nullable
            OpponentDto opponent,

            @Schema(description = "Absent tant que le duel n'est pas réglé") @Nullable
            DuelResultDto result) {

        static DuelDto from(DuelView view) {
            Duel duel = view.duel();
            DuelPlayer opponent = view.opponent();
            Result result = view.result();
            return new DuelDto(
                    duel.id(),
                    duel.subjectSlug(),
                    duel.mode(),
                    duel.status(),
                    duel.cardIds().size(),
                    duel.expiresAt(),
                    DuelSideDto.from(view.me()),
                    opponent == null ? null : OpponentDto.from(opponent, view.opponentName()),
                    result == null ? null : DuelResultDto.from(result));
        }
    }

    @Schema(name = "DuelSide")
    record DuelSideDto(
            @Schema(description = "Les questions répondues, ou dont le temps est passé")
            int answered,

            @Schema(description = "Les bonnes réponses, à temps")
            int correct,

            boolean finished) {

        static DuelSideDto from(DuelPlayer player) {
            int correct = (int)
                    player.answers().stream().filter(Duel.DuelAnswer::correct).count();
            return new DuelSideDto(player.answers().size(), correct, player.hasFinished());
        }
    }

    @Schema(name = "DuelOpponent", description = "L'adversaire : ses bonnes réponses ne se voient qu'au résultat")
    record OpponentDto(@Nullable String displayName, int answered, boolean finished) {

        static OpponentDto from(DuelPlayer player, @Nullable String displayName) {
            return new OpponentDto(displayName, player.answers().size(), player.hasFinished());
        }
    }

    @Schema(name = "DuelResult")
    record DuelResultDto(
            Outcome outcome,
            int myCorrect,
            int opponentCorrect,
            long myTimeMs,
            long opponentTimeMs,

            @Schema(description = "Vrai pendant le placement : les rangs restent cachés")
            boolean placing,

            @Nullable RankDto rankBefore,
            @Nullable RankDto rankAfter,

            @Schema(description = "Vrai si le joueur est monté d'un rang")
            boolean promoted) {

        static DuelResultDto from(Result result) {
            return new DuelResultDto(
                    Outcome.of(result.score()),
                    result.mine().correct(),
                    result.theirs().correct(),
                    result.mine().timeMs(),
                    result.theirs().timeMs(),
                    result.placing(),
                    RankDto.from(result.before()),
                    RankDto.from(result.after()),
                    result.promoted());
        }
    }

    @Schema(name = "DuelTurn")
    record TurnDto(
            @Schema(description = "Vrai quand le joueur a joué toutes ses questions")
            boolean finished,

            @Schema(description = "Le rang de la question, à partir de 0 ; absent une fois fini") @Nullable
            Integer position,

            int total,
            @Nullable DuelQuestionDto question,

            @Schema(description = "Le temps qui reste pour répondre, mesuré par le serveur") @Nullable
            Long remainingMs) {

        static TurnDto finished(int total) {
            return new TurnDto(true, null, total, null, null);
        }

        static TurnDto from(Turn turn) {
            return new TurnDto(
                    false,
                    turn.position(),
                    turn.total(),
                    DuelQuestionDto.from(turn.card()),
                    turn.remaining().toMillis());
        }
    }

    @Schema(name = "DuelQuestion")
    record DuelQuestionDto(
            UUID cardId,
            CardType type,

            @Schema(description = "En Markdown ; les trous d'un code à compléter sont des ___")
            String prompt,

            @Schema(description = "Les choix, sans dire lesquels sont justes")
            List<String> choices,

            boolean multipleAnswers,
            int blankCount) {

        static DuelQuestionDto from(CardSnapshot card) {
            return new DuelQuestionDto(
                    card.cardId(),
                    card.type(),
                    card.prompt(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    card.blanks().size());
        }
    }

    @Schema(name = "DuelAnswerResult")
    record AnswerResultDto(
            @Schema(description = "Vrai si la réponse compte juste : juste, et à temps")
            boolean correct,

            @Schema(description = "Vrai si elle est arrivée après le temps imparti")
            boolean late,

            List<Integer> correctChoices,
            List<BlankResultDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation,

            @Schema(description = "Vrai si c'était la dernière question du joueur")
            boolean finished) {

        static AnswerResultDto from(Answered answered) {
            return new AnswerResultDto(
                    answered.counted(),
                    answered.late(),
                    answered.correction().correctChoices().stream().sorted().toList(),
                    answered.correction().blanks().stream()
                            .map(BlankResultDto::from)
                            .toList(),
                    answered.correction().explanation(),
                    answered.finished());
        }
    }

    @Schema(name = "DuelBlankResult")
    record BlankResultDto(String expected, boolean correct) {

        static BlankResultDto from(BlankResult blank) {
            return new BlankResultDto(blank.expected(), blank.correct());
        }
    }

    @Schema(name = "DuelSummary")
    record DuelSummaryDto(
            UUID id,
            String subject,
            Mode mode,
            Status status,
            Instant expiresAt,
            @Nullable String opponentName,

            @Schema(description = "Absent tant que le duel n'est pas réglé") @Nullable
            Outcome outcome,

            boolean myTurnDone) {

        static DuelSummaryDto from(DuelView view) {
            Result result = view.result();
            return new DuelSummaryDto(
                    view.duel().id(),
                    view.duel().subjectSlug(),
                    view.duel().mode(),
                    view.duel().status(),
                    view.duel().expiresAt(),
                    view.opponentName(),
                    result == null ? null : Outcome.of(result.score()),
                    view.me().hasFinished());
        }
    }

    @Schema(name = "LiveQueueStatus")
    record LiveQueueDto(
            @Schema(description = "Vrai tant que le joueur attend un adversaire")
            boolean waiting,

            @Schema(description = "Le duel trouvé ; absent tant qu'il attend") @Nullable
            UUID duelId) {}
}
