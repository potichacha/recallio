package com.recallio.review.domain.exam;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.review.domain.exam.MockExamDraw.Candidate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockExamDrawTest {

    private static final UUID ESSENTIAL = UUID.fromString("00000000-0000-7000-8000-000000000001");
    private static final UUID COMMON = UUID.fromString("00000000-0000-7000-8000-000000000002");
    private static final UUID RARE = UUID.fromString("00000000-0000-7000-8000-000000000003");

    private static Candidate card(int number, UUID notion, int priority) {
        return new Candidate(new UUID(0, number), notion, priority);
    }

    private static final List<Candidate> CANDIDATES = List.of(
            card(1, RARE, 4),
            card(2, ESSENTIAL, 1),
            card(3, COMMON, 2),
            card(4, ESSENTIAL, 1),
            card(5, ESSENTIAL, 1),
            card(6, COMMON, 2));

    @Test
    void shouldAskOneQuestionPerNotionEssentialFirstBeforeASecondOne() {
        List<UUID> drawn = MockExamDraw.draw(CANDIDATES, 4, 42);

        assertThat(drawn).hasSize(4).doesNotHaveDuplicates();
        // Le premier tour : une essentielle, une courante, la rare ; le second commence par une essentielle.
        assertThat(notionsOf(drawn)).containsExactly(ESSENTIAL, COMMON, RARE, ESSENTIAL);
    }

    @Test
    void shouldDrawTheSameQuestionsFromTheSameSeed() {
        assertThat(MockExamDraw.draw(CANDIDATES, 6, 7)).isEqualTo(MockExamDraw.draw(CANDIDATES, 6, 7));
        assertThat(MockExamDraw.draw(CANDIDATES, 10, 7)).hasSize(6);
    }

    @Test
    void shouldCountACardWithoutNotionAsItsOwnNotion() {
        var lonely = new Candidate(new UUID(0, 9), null, 2);

        assertThat(MockExamDraw.draw(List.of(lonely, card(2, ESSENTIAL, 1)), 2, 1))
                .containsExactly(new UUID(0, 2), new UUID(0, 9));
    }

    @Test
    void shouldGiveOneMinutePerQuestionAndThirtySecondsOfGrace() {
        Instant now = Instant.parse("2026-11-05T10:00:00Z");
        MockExam exam =
                MockExam.start(new UUID(0, 1), new UUID(0, 2), "go", List.of(new UUID(0, 3), new UUID(0, 4)), now);

        assertThat(exam.deadline()).isEqualTo(now.plus(Duration.ofMinutes(2)));
        assertThat(exam.remaining(now.plusSeconds(30))).isEqualTo(Duration.ofSeconds(90));
        assertThat(exam.remaining(now.plus(Duration.ofHours(1)))).isZero();
        assertThat(exam.inTime(exam.deadline().plusSeconds(30))).isTrue();
        assertThat(exam.inTime(exam.deadline().plusSeconds(31))).isFalse();
        assertThat(new MockExam.Submission(now, 2, 3).percent()).isEqualTo(67);
    }

    private static List<UUID> notionsOf(List<UUID> cards) {
        return cards.stream()
                .map(card -> CANDIDATES.stream()
                        .filter(candidate -> candidate.cardId().equals(card))
                        .findFirst()
                        .orElseThrow()
                        .notionId())
                .toList();
    }
}
