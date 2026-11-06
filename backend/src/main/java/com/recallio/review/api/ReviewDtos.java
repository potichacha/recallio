package com.recallio.review.api;

import com.recallio.content.CardBlank;
import com.recallio.content.CardChoice;
import com.recallio.content.CardType;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.application.ReviewQueueService.QueuedCard;
import com.recallio.review.application.ReviewQueueService.ReviewQueue;
import com.recallio.review.application.ReviewRecordingService.BlankCorrection;
import com.recallio.review.application.ReviewRecordingService.Correction;
import com.recallio.review.application.ReviewRecordingService.ReviewOutcome;
import com.recallio.review.domain.LearningState;
import com.recallio.review.domain.Rating;
import com.recallio.review.domain.exercise.ExerciseRun;
import com.recallio.review.domain.exercise.ExerciseRun.TestResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Contrat JSON du module review : le domaine n'est jamais exposé directement. */
final class ReviewDtos {

    /** Au-delà d'une heure, le temps de réflexion n'a plus de sens (application laissée ouverte). */
    static final int MAX_ELAPSED_MS = 3_600_000;
    /** Longueur maximale de la réponse d'un trou : une expression, pas un programme. */
    static final int MAX_BLANK_LENGTH = 200;

    private ReviewDtos() {}

    @Schema(name = "ReviewQueue")
    record ReviewQueueDto(
            List<QueuedCardDto> cards,

            @Schema(description = "Cartes dues au total, la file peut en contenir moins")
            int dueCount,

            @Schema(description = "Cartes nouvelles encore disponibles aujourd'hui")
            int newCount,

            @Schema(description = "Prochaine échéance quand plus rien n'est dû", nullable = true) @Nullable
            Instant nextDueAt) {
        static ReviewQueueDto from(ReviewQueue queue) {
            return new ReviewQueueDto(
                    queue.cards().stream().map(QueuedCardDto::from).toList(),
                    queue.dueCount(),
                    queue.newCount(),
                    queue.nextDueAt());
        }
    }

    @Schema(name = "QueuedCard")
    record QueuedCardDto(
            UUID cardId,
            CardType type,
            String subjectSlug,
            String subjectTitle,
            String themeSlug,
            String themeTitle,
            @Schema(description = "Recto, en Markdown") String prompt,

            @Schema(
                    description = "Verso, en Markdown ; absent pour une carte corrigée par le serveur (une carte à"
                            + " choix, un code à trous, un exercice), dont l'explication arrive avec la correction",
                    nullable = true)
            @Nullable
            String answer,

            @Schema(
                    description = "Les choix de la carte, en Markdown, sans dire lesquels sont justes ; vide pour"
                            + " une carte qui s'auto-évalue, un code à trous ou un exercice (ADR 0051)")
            List<String> choices,

            @Schema(
                    description =
                            "Vrai si le QCM a plusieurs bonnes réponses (cases à cocher plutôt que boutons radio)")
            boolean multipleAnswers,

            @Schema(description = "Le nombre de trous d'un code à trous, à remplir dans l'ordre ; 0 sinon")
            int blankCount,

            @Schema(
                    description = "Les fichiers d'un exercice de code ; absent pour les autres formats",
                    nullable = true)
            @Nullable
            ExerciseToSolveDto exercise,

            @Schema(
                    description = "Le titre de la notion de la carte ; absent hors d'un dossier de sujet",
                    nullable = true)
            @Nullable
            String notionTitle,

            @Schema(
                    description = "La leçon de la notion, en Markdown, à lire avant sa première carte en parcours"
                            + " « apprendre » (ADR 0032) ; absente le plus souvent",
                    nullable = true)
            @Nullable
            String lesson,

            LearningState state) {
        static QueuedCardDto from(QueuedCard queued) {
            var card = queued.card();
            return new QueuedCardDto(
                    card.cardId(),
                    card.type(),
                    card.subjectSlug(),
                    card.subjectTitle(),
                    card.themeSlug(),
                    card.themeTitle(),
                    card.prompt(),
                    card.isGradedByServer() ? null : card.answer(),
                    card.choices().stream().map(CardChoice::text).toList(),
                    card.correctChoices().size() > 1,
                    card.blanks().size(),
                    card.exercise() == null ? null : ExerciseToSolveDto.from(card.exercise()),
                    card.notionTitle(),
                    queued.lesson(),
                    queued.state());
        }
    }

    /**
     * Un exercice à résoudre. Auto-évalué tant que le code n'est pas exécuté (ADR 0029) : la solution vient avec lui,
     * comme la réponse d'une carte auto-évaluée, et la PWA ne la montre qu'une fois révélée.
     */
    @Schema(name = "ExerciseToSolve")
    record ExerciseToSolveDto(
            @Schema(description = "Le code de départ, à modifier")
            List<ExerciseFileDto> starter,

            @Schema(description = "Les tests qui jugent le code")
            List<ExerciseFileDto> tests) {
        static ExerciseToSolveDto from(ExerciseFiles files) {
            return new ExerciseToSolveDto(ExerciseFileDto.from(files.starter()), ExerciseFileDto.from(files.tests()));
        }
    }

    /**
     * Un fichier de code : celui d'un exercice, envoyé au joueur (code de départ, tests, solution) ou rendu par lui, ou
     * celui d'un programme du Tour qu'il exécute (ADR 0037). Un fichier vide n'est jamais valable.
     */
    @Schema(name = "ExerciseFile")
    record ExerciseFileDto(
            @Schema(description = "Chemin relatif, avec des /") @NotNull @Pattern(regexp = SourceFile.PATH_PATTERN)
            String path,

            @NotBlank @Size(max = SourceFile.MAX_LENGTH) String content) {

        SourceFile toSourceFile() {
            return new SourceFile(path, content);
        }

        static List<ExerciseFileDto> from(List<SourceFile> files) {
            return files.stream()
                    .map(file -> new ExerciseFileDto(file.path(), file.content()))
                    .toList();
        }
    }

    @Schema(name = "RecordReviewRequest")
    record RecordReviewRequest(
            @Schema(description = "Identifiant généré par le client, réutilisé tel quel si la requête est renvoyée")
            @NotNull
            UUID requestId,

            @NotNull UUID cardId,

            @Schema(
                    description = "La note d'auto-évaluation ; absente pour un QCM, que le serveur corrige",
                    nullable = true)
            @Nullable
            Rating rating,

            @Schema(
                    description = "Pour un QCM seulement : les positions des choix cochés, à partir de 0",
                    nullable = true)
            @Size(max = CardChoice.MAX_PER_CARD)
            @Nullable
            List<@NotNull @PositiveOrZero Integer> selectedChoices,

            @Schema(
                    description = "Pour un code à trous seulement : la réponse de chaque trou, dans l'ordre",
                    nullable = true)
            @Size(max = CardBlank.MAX_PER_CARD)
            @Nullable
            List<@NotNull @Size(max = MAX_BLANK_LENGTH) String> filledBlanks,

            @Schema(
                    description = "Pour un exercice de code seulement : les fichiers rendus, exactement ceux du code de"
                            + " départ. Le serveur les exécute contre les tests et en déduit la note",
                    nullable = true)
            @Size(max = ExerciseFiles.MAX_FILES_PER_PART)
            @Nullable
            List<@NotNull @Valid ExerciseFileDto> submittedFiles,

            @Schema(description = "Temps de réflexion en millisecondes", nullable = true)
            @PositiveOrZero
            @Max(MAX_ELAPSED_MS)
            @Nullable
            Integer elapsedMs) {}

    @Schema(name = "ReviewResult")
    record ReviewResultDto(
            UUID cardId,

            @Schema(description = "La note enregistrée ; pour un QCM, celle que donne la correction")
            Rating rating,

            LearningState state,
            Instant dueAt,

            @Schema(description = "La correction d'un QCM ; absente pour les autres formats", nullable = true) @Nullable
            CorrectionDto correction) {
        static ReviewResultDto from(ReviewOutcome outcome) {
            Correction correction = outcome.correction();
            return new ReviewResultDto(
                    outcome.cardId(),
                    outcome.rating(),
                    outcome.state(),
                    outcome.dueAt(),
                    correction == null ? null : CorrectionDto.from(correction));
        }
    }

    @Schema(name = "Correction")
    record CorrectionDto(
            @Schema(
                    description =
                            "Positions des bonnes réponses d'un QCM ou d'un vrai ou faux, à partir de 0, dans l'ordre")
            List<Integer> correctChoices,

            @Schema(description = "La correction de chaque trou d'un code à trous, dans l'ordre ; vide sinon")
            List<BlankCorrectionDto> blanks,

            @Schema(description = "L'explication, en Markdown")
            String explanation,

            @Schema(description = "Le résultat des tests d'un exercice de code ; absent sinon", nullable = true)
            @Nullable
            ExerciseRunDto exerciseRun,

            @Schema(description = "La solution modèle d'un exercice de code, une fois rendu ; vide sinon")
            List<ExerciseFileDto> solution) {
        static CorrectionDto from(Correction correction) {
            ExerciseRun run = correction.exerciseRun();
            return new CorrectionDto(
                    correction.correctChoices().stream().sorted().toList(),
                    correction.blanks().stream().map(BlankCorrectionDto::from).toList(),
                    correction.explanation(),
                    run == null ? null : ExerciseRunDto.from(run),
                    ExerciseFileDto.from(correction.solution()));
        }
    }

    @Schema(name = "ExerciseRunRequest")
    record ExerciseRunRequest(
            @NotNull UUID cardId,

            @Schema(description = "Les fichiers à essayer : exactement ceux du code de départ")
            @NotNull
            @Size(min = 1, max = ExerciseFiles.MAX_FILES_PER_PART)
            List<@NotNull @Valid ExerciseFileDto> files) {}

    @Schema(name = "ExerciseRun")
    record ExerciseRunDto(
            ExerciseRun.Status status,

            @Schema(
                    description =
                            "Chaque test et son résultat ; vide si le code n'a pas compilé ou n'a pas fini à temps")
            List<TestResultDto> tests,

            @Schema(description = "Les erreurs de compilation, ou ce qui a arrêté les tests ; vide sinon")
            String output) {
        static ExerciseRunDto from(ExerciseRun run) {
            return new ExerciseRunDto(
                    run.status(), run.tests().stream().map(TestResultDto::from).toList(), run.output());
        }
    }

    @Schema(name = "TestResult")
    record TestResultDto(
            String className,
            String name,
            boolean passed,

            @Schema(description = "Pourquoi le test échoue ; vide s'il passe")
            String message) {
        static TestResultDto from(TestResult test) {
            return new TestResultDto(test.className(), test.name(), test.passed(), test.message());
        }
    }

    @Schema(name = "BlankCorrection")
    record BlankCorrectionDto(
            @Schema(description = "La réponse attendue") String expected,

            @Schema(description = "Vrai si la réponse donnée est acceptée")
            boolean correct) {
        static BlankCorrectionDto from(BlankCorrection blank) {
            return new BlankCorrectionDto(blank.expected(), blank.correct());
        }
    }
}
