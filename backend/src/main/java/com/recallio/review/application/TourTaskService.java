package com.recallio.review.application;

import com.recallio.content.ExerciseFiles.SourceFile;
import com.recallio.content.TourDirectory;
import com.recallio.content.TourPage;
import com.recallio.review.domain.AnswerMismatchException;
import com.recallio.review.domain.playground.ProgramRun;
import com.recallio.review.domain.playground.ProgramRunInProgressException;
import com.recallio.review.domain.playground.ProgramRunner;
import com.recallio.review.domain.playground.ProgramStatus;
import com.recallio.review.domain.tour.TourTaskNotFoundException;
import com.recallio.user.UserRef;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cas d'usage : juger un « À toi » du Tour (ADR 0037). Le programme de l'utilisateur s'exécute dans le bac à sable, et
 * sa sortie est comparée à celle attendue, qui ne quitte pas le serveur. Rien n'est enregistré, et l'on peut réessayer
 * autant de fois qu'on veut ; une exécution à la fois par utilisateur ({@link RunLock}).
 */
@ApplicationScoped
public class TourTaskService {

    private final TourDirectory tours;
    private final ProgramRunner runner;
    private final RunLock lock;

    public TourTaskService(TourDirectory tours, ProgramRunner runner, RunLock lock) {
        this.tours = tours;
        this.runner = runner;
        this.lock = lock;
    }

    /**
     * @param page le rang de la page dans sa notion, à partir de 1
     * @throws TourTaskNotFoundException si la page n'existe pas, ou n'a pas d'« À toi »
     * @throws AnswerMismatchException si les fichiers ne sont pas ceux du code de départ
     * @throws ProgramRunInProgressException si l'utilisateur a déjà une exécution en cours
     */
    public TaskCheck check(UserRef user, String subjectSlug, String notionSlug, int page, List<SourceFile> files) {
        TourPage.Task task = tours.page(subjectSlug, notionSlug, page)
                .flatMap(found -> Optional.ofNullable(found.task()))
                .orElseThrow(() -> new TourTaskNotFoundException(subjectSlug, notionSlug, page));
        Set<String> expected = paths(task.starter());
        if (!paths(files).equals(expected) || files.size() != expected.size()) {
            throw AnswerMismatchException.starterFilesExpected(
                    expected.stream().sorted().toList());
        }
        ProgramRun run =
                lock.alone(user.id(), () -> runner.run(task.language(), files), ProgramRunInProgressException::new);
        return new TaskCheck(passes(run, task.output()), run);
    }

    /** Le programme est allé au bout, sans erreur, et il a écrit la sortie attendue. */
    static boolean passes(ProgramRun run, String expected) {
        return run.status() == ProgramStatus.COMPLETED
                && run.exitCode() == 0
                && !run.truncated()
                && normalized(run.stdout()).equals(normalized(expected));
    }

    /** La comparaison de la CI ({@code TourTest}) : aux fins de ligne et aux blancs finaux près. */
    private static String normalized(String output) {
        return output.replace("\r\n", "\n").stripTrailing();
    }

    private static Set<String> paths(List<SourceFile> files) {
        return files.stream().map(SourceFile::path).collect(Collectors.toSet());
    }

    /** @param passed vrai si la sortie est celle attendue */
    public record TaskCheck(boolean passed, ProgramRun run) {}
}
