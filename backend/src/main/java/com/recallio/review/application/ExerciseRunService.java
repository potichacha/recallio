package com.recallio.review.application;

import com.recallio.content.CardDirectory;
import com.recallio.content.CardDirectory.CardSnapshot;
import com.recallio.content.ExerciseFiles;
import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.CardNotReviewableException;
import com.recallio.review.domain.exercise.CodeRunner;
import com.recallio.review.domain.exercise.ExerciseRun;
import com.recallio.review.domain.exercise.ExerciseRunInProgressException;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cas d'usage : exécuter le code d'un joueur contre les tests d'un exercice (ADR 0030), pour l'essayer autant de fois
 * qu'il veut, ou pour le juger quand il le rend. Ne touche pas à la progression.
 *
 * <p>Une exécution à la fois par utilisateur, programmes du Tour compris ({@link RunLock}) : un joueur ne monopolise pas
 * le runner, qui borne aussi le total.
 */
@ApplicationScoped
public class ExerciseRunService {

    private final CardDirectory cards;
    private final CodeRunner runner;
    private final RunLock lock;

    public ExerciseRunService(CardDirectory cards, CodeRunner runner, RunLock lock) {
        this.cards = cards;
        this.runner = runner;
        this.lock = lock;
    }

    /**
     * @throws CardNotReviewableException si la carte est inconnue ou retirée
     * @throws AnswerMismatchException si ce n'est pas un exercice, ou si les fichiers ne sont pas ceux du code de départ
     * @throws ExerciseRunInProgressException si l'utilisateur a déjà une exécution en cours
     */
    public ExerciseRun run(UserRef user, UUID cardId, List<SourceFile> files) {
        CardSnapshot card = cards.activeCard(cardId).orElseThrow(() -> new CardNotReviewableException(cardId));
        return run(user, card, files);
    }

    ExerciseRun run(UserRef user, CardSnapshot card, List<SourceFile> files) {
        ExerciseFiles exercise = card.exercise();
        if (exercise == null) {
            throw AnswerMismatchException.notAnExercise();
        }
        Set<String> expected = paths(exercise.starter());
        if (!paths(files).equals(expected) || files.size() != expected.size()) {
            throw AnswerMismatchException.starterFilesExpected(
                    expected.stream().sorted().toList());
        }
        return lock.alone(
                user.id(),
                () -> runner.run(exercise.language(), files, exercise.tests()),
                ExerciseRunInProgressException::new);
    }

    private static Set<String> paths(List<SourceFile> files) {
        return files.stream().map(SourceFile::path).collect(Collectors.toSet());
    }
}
