package com.recallio.review.domain.exam;

import com.recallio.review.domain.exam.MockExam.Submission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Les examens blancs (ADR 0044), dans les tables {@code mock_exam} et {@code mock_exam_question}. */
public interface MockExamRepository {

    void add(MockExam exam);

    Optional<MockExam> find(UUID id);

    /** @return faux si la copie était déjà rendue : la première note reste */
    boolean submit(UUID id, Submission submission);

    /** Les dernières copies rendues de l'utilisateur dans ce sujet, la plus récente d'abord. */
    List<MockExam> submitted(UUID userId, String subjectSlug, int limit);
}
