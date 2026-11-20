package com.recallio.review.domain.daily;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * La tentative d'un joueur à un défi du jour (ADR 0067) : ses réponses, question par question. Une question dont la
 * carte a été retirée depuis le tirage n'est plus posée : le défi se finit, et se compte, sur les autres.
 *
 * @param questions les positions des questions encore posées, dans l'ordre
 * @param answers le verdict de chaque réponse du joueur, par position
 */
public record DailyChallengeAttempt(List<Integer> questions, Map<Integer, Boolean> answers) {

    public DailyChallengeAttempt {
        questions = List.copyOf(questions);
        answers = Map.copyOf(answers);
    }

    /** La tentative d'après les réponses gardées du joueur. */
    public static DailyChallengeAttempt of(List<Integer> questions, Collection<DailyChallengeAnswer> answers) {
        return new DailyChallengeAttempt(
                questions,
                answers.stream()
                        .collect(Collectors.toMap(DailyChallengeAnswer::position, DailyChallengeAnswer::correct)));
    }

    public DailyChallengeState state() {
        if (answers.isEmpty()) {
            return DailyChallengeState.NOT_STARTED;
        }
        return answers.keySet().containsAll(questions) ? DailyChallengeState.DONE : DailyChallengeState.IN_PROGRESS;
    }

    /** Vrai dès la première réponse : un défi commencé avant minuit se finit après. */
    public boolean isStarted() {
        return !answers.isEmpty();
    }

    public boolean isDone() {
        return state() == DailyChallengeState.DONE;
    }

    public boolean isAnswered(int position) {
        return answers.containsKey(position);
    }

    /** Les questions justes, parmi celles encore posées. */
    public int score() {
        return (int) questions.stream()
                .filter(position -> Boolean.TRUE.equals(answers.get(position)))
                .count();
    }

    /** Le nombre de questions du défi : cinq, sauf carte retirée depuis le tirage. */
    public int total() {
        return questions.size();
    }
}
