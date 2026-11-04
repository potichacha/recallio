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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Cas d'usage : exécuter le code d'un joueur contre les tests d'un exercice (ADR 0030), pour l'essayer autant de fois
 * qu'il veut, ou pour le juger quand il le rend. Ne touche pas à la progression.
 *
 * <p>Une exécution à la fois par utilisateur : un joueur ne monopolise pas le runner, qui borne aussi le total. Le
 * verrou est en mémoire, ce qui suffit pour un backend unique (CLAUDE.md, section 4.1).
 */
@ApplicationScoped
public class ExerciseRunService {

    private final CardDirectory cards;
    private final CodeRunner runner;
    private final Set<UUID> running = ConcurrentHashMap.newKeySet();

    public ExerciseRunService(CardDirectory cards, CodeRunner runner) {
        this.cards = cards;
        this.runner = runner;
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
        if (!running.add(user.id())) {
            throw new ExerciseRunInProgressException();
        }
        try {
            return runner.run(exercise.language(), files, exercise.tests());
        } finally {
            running.remove(user.id());
        }
    }

    private static Set<String> paths(List<SourceFile> files) {
        return files.stream().map(SourceFile::path).collect(Collectors.toSet());
    }
}
