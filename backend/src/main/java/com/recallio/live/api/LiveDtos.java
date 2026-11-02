package com.recallio.live.api;

import com.recallio.content.CardChoice;
import com.recallio.live.domain.LiveGame.PlayerView;
import com.recallio.live.domain.LiveGame.Round;
import com.recallio.live.domain.LiveGame.Snapshot;
import com.recallio.live.domain.LiveGameStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Contrat JSON du mode live, en REST comme sur le WebSocket (ADR 0021 et 0022). */
final class LiveDtos {

    private LiveDtos() {}

    @Schema(name = "OpenLiveGameRequest")
    record OpenLiveGameRequest(
            @Schema(description = "Slug d'un des sujets du groupe") @NotBlank
            String subject) {}

    @Schema(name = "AnswerLiveQuestionRequest")
    record AnswerLiveQuestionRequest(
            @Schema(description = "Position de la question, à partir de 0") @PositiveOrZero
            int question,

            @Schema(description = "Positions des choix cochés, à partir de 0")
            @NotNull
            @Size(max = CardChoice.MAX_PER_CARD)
            List<@NotNull @PositiveOrZero Integer> selectedChoices) {}

    /** @param now l'instant d'envoi, pour le temps restant : le client ne se fie jamais à sa propre horloge */
    @Schema(name = "LiveGame")
    record LiveGameDto(
            UUID id,
            UUID groupId,
            String subject,
            UUID hostId,
            Instant createdAt,
            LiveGameStatus status,

            @Schema(description = "Les joueurs connectés, dans l'ordre d'arrivée")
            List<LivePlayerDto> players,

            @Schema(
                    description = "La question en cours, ou la dernière posée ; absente en salle d'attente",
                    nullable = true)
            @Nullable
            LiveRoundDto round) {
        static LiveGameDto from(Snapshot game, Instant now) {
            Round round = game.round();
            return new LiveGameDto(
                    game.id(),
                    game.groupId(),
                    game.subjectSlug(),
                    game.hostId(),
                    game.createdAt(),
                    game.status(),
                    game.players().stream().map(LivePlayerDto::from).toList(),
                    round == null ? null : LiveRoundDto.from(round, game.status(), now));
        }
    }

    @Schema(name = "LivePlayer")
    record LivePlayerDto(
            UUID userId,
            String displayName,
            boolean host,

            @Schema(description = "A répondu à la question en cours")
            boolean answered) {
        static LivePlayerDto from(PlayerView player) {
            return new LivePlayerDto(player.userId(), player.displayName(), player.host(), player.answered());
        }
    }

    /** Une question vue par les joueurs : ses bonnes réponses n'apparaissent qu'à la correction. */
    @Schema(name = "LiveRound")
    record LiveRoundDto(
            @Schema(description = "Position de la question, à partir de 0")
            int index,

            int total,
            @Schema(description = "En Markdown") String prompt,

            @Schema(description = "En Markdown, sans dire lesquels sont justes")
            List<String> choices,

            boolean multipleAnswers,

            @Schema(description = "Temps restant avant la fin du chrono, ou de la correction")
            long remainingMs,

            int answeredCount,

            @Schema(description = "La correction, une fois la question close", nullable = true) @Nullable
            LiveRevealDto reveal) {
        static LiveRoundDto from(Round round, LiveGameStatus status, Instant now) {
            var question = round.question();
            Instant endsAt = round.endsAt();
            long remaining = endsAt == null
                    ? 0
                    : Math.max(0, Duration.between(now, endsAt).toMillis());
            boolean revealed = status == LiveGameStatus.REVEAL || status == LiveGameStatus.FINISHED;
            return new LiveRoundDto(
                    round.index(),
                    round.total(),
                    question.prompt(),
                    question.choices(),
                    question.multipleAnswers(),
                    remaining,
                    round.answeredCount(),
                    revealed
                            ? new LiveRevealDto(
                                    question.correctChoices().stream().sorted().toList(),
                                    question.explanation(),
                                    round.correctCount())
                            : null);
        }
    }

    @Schema(name = "LiveReveal")
    record LiveRevealDto(
            List<Integer> correctChoices,
            @Schema(description = "En Markdown") String explanation,
            @Schema(description = "Joueurs qui ont trouvé") int correctCount) {}

    /**
     * Un message du serveur sur le WebSocket. {@code type} dit comment le lire ; aujourd'hui, l'état de la partie
     * ({@code game}), à chaque changement.
     */
    record LiveEventDto(String type, LiveGameDto game) {
        static final String GAME = "game";

        static LiveEventDto game(Snapshot game, Instant now) {
            return new LiveEventDto(GAME, LiveGameDto.from(game, now));
        }
    }
}
