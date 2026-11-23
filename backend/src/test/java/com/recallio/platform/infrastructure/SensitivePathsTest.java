package com.recallio.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Ce qui ne quitte jamais le serveur dans une erreur envoyée à Sentry (ADR 0070). */
class SensitivePathsTest {

    @Test
    void shouldMaskTheTokenOfAnInvitationInTheMessageOfTheQuarkusErrorHandler() {
        String message = "HTTP Request to /api/v1/invitations/Zx9-kq_3TfA/accept failed, error id: 4f1c-2";

        assertThat(SensitivePaths.mask(message))
                .isEqualTo("HTTP Request to /api/v1/invitations/:token/accept failed, error id: 4f1c-2");
    }

    @Test
    void shouldMaskTheTokenOfAnInvitationAtTheEndOfTheText() {
        assertThat(SensitivePaths.mask("GET /api/v1/invitations/Zx9-kq_3TfA"))
                .isEqualTo("GET /api/v1/invitations/:token");
    }

    @Test
    void shouldMaskTheHandleOfAPublicProfile() {
        assertThat(SensitivePaths.mask("HTTP Request to /api/v1/public-profiles/alice-dupont failed"))
                .isEqualTo("HTTP Request to /api/v1/public-profiles/:handle failed");
    }

    @Test
    void shouldMaskTheQueryString() {
        assertThat(SensitivePaths.mask("HTTP Request to /api/v1/subjects/java/cards?search=alice&page=2 failed"))
                .isEqualTo("HTTP Request to /api/v1/subjects/java/cards failed");
    }

    @Test
    void shouldMaskTheQueryStringAndTheTokenTogether() {
        assertThat(SensitivePaths.mask("/api/v1/invitations/Zx9-kq_3TfA?code=abc&state=def"))
                .isEqualTo("/api/v1/invitations/:token");
    }

    @Test
    void shouldMaskEveryOccurrence() {
        assertThat(SensitivePaths.mask("/api/v1/public-profiles/alice puis /api/v1/public-profiles/bob"))
                .isEqualTo("/api/v1/public-profiles/:handle puis /api/v1/public-profiles/:handle");
    }

    @Test
    void shouldKeepTheIdentifiersOfResources() {
        String message = "HTTP Request to /api/v1/live-games/0192f5e4-7b1a-7c3e-9d2f-1a2b3c4d5e6f/answers failed";

        assertThat(SensitivePaths.mask(message)).isEqualTo(message);
    }

    @Test
    void shouldKeepTheQuestionMarkOfASentence() {
        assertThat(SensitivePaths.mask("Le runner est-il joignable ? Non."))
                .isEqualTo("Le runner est-il joignable ? Non.");
    }

    @Test
    void shouldKeepTheRouteThatCreatesTheInvitationOfAGroup() {
        assertThat(SensitivePaths.mask("POST /api/v1/groups/0192f5e4-7b1a-7c3e-9d2f-1a2b3c4d5e6f/invitation"))
                .isEqualTo("POST /api/v1/groups/0192f5e4-7b1a-7c3e-9d2f-1a2b3c4d5e6f/invitation");
    }
}
