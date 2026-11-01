package com.recallio.group.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GroupInvitationTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    @Test
    void shouldExpireSevenDaysAfterItsCreation() {
        GroupInvitation invitation = GroupInvitation.create(UUID.randomUUID(), InvitationToken.generate(), NOW);

        assertThat(invitation.expiresAt()).isEqualTo(Instant.parse("2026-11-08T10:00:00Z"));
        assertThat(invitation.isActiveAt(Instant.parse("2026-11-08T09:59:59Z"))).isTrue();
        assertThat(invitation.isActiveAt(invitation.expiresAt())).isFalse();
    }

    @Test
    void shouldGenerateUrlSafeTokensThatNeverRepeat() {
        var tokens = IntStream.range(0, 1_000)
                .mapToObj(i -> InvitationToken.generate().value())
                .toList();

        assertThat(tokens).doesNotHaveDuplicates().allMatch(token -> token.matches("[A-Za-z0-9_-]{32}"));
    }

    @Test
    void shouldIgnoreMalformedTokens() {
        assertThat(InvitationToken.parse("trop-court")).isEmpty();
        assertThat(InvitationToken.parse("a".repeat(31) + "/")).isEmpty();
        assertThat(InvitationToken.parse("a".repeat(32))).contains(new InvitationToken("a".repeat(32)));
        assertThatThrownBy(() -> new InvitationToken("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNeverPrintTheSecret() {
        InvitationToken token = InvitationToken.generate();

        assertThat(token.toString()).doesNotContain(token.value());
    }
}
