package com.recallio.review.domain.interview;

import java.util.Map;
import java.util.UUID;

/** Les entretiens prévus (ADR 0066), dans la table {@code interview_plan} : un par utilisateur et par sujet. */
public interface InterviewPlanRepository {

    /** Fixe la date d'un sujet, ou remplace la précédente : changer la date repart de zéro. */
    void save(UUID userId, InterviewPlan plan);

    /** Les entretiens prévus de l'utilisateur, par slug de sujet, que le sujet soit encore suivi ou non. */
    Map<String, InterviewPlan> findAll(UUID userId);

    /** Retire la date d'un sujet ; ne fait rien si elle n'existait pas. */
    void delete(UUID userId, String subjectSlug);
}
