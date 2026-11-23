package com.recallio.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Une erreur part chez Sentry sans jeton d'invitation, adresse de profil ni chaîne de requête (ADR 0070). */
class SentryEventScrubberTest {

    private static final String URI = "/api/v1/invitations/Zx9-kq_3TfA/accept?code=abc";
    private static final String MASKED_URI = "/api/v1/invitations/:token/accept";

    private final SentryEventScrubber scrubber = new SentryEventScrubber();

    @Test
    void shouldMaskTheMessageOfTheEventAndItsParameters() {
        var message = new Message();
        message.setMessage("HTTP Request to %s failed, error id: %s");
        message.setParams(List.of(URI, "4f1c-2"));
        message.setFormatted("HTTP Request to " + URI + " failed, error id: 4f1c-2");
        var event = new SentryEvent();
        event.setMessage(message);

        Message sent =
                Objects.requireNonNull(scrubber.execute(event, new Hint()).getMessage());

        assertThat(sent.getMessage()).isEqualTo("HTTP Request to %s failed, error id: %s");
        assertThat(sent.getParams()).containsExactly(MASKED_URI, "4f1c-2");
        assertThat(sent.getFormatted()).isEqualTo("HTTP Request to " + MASKED_URI + " failed, error id: 4f1c-2");
    }

    @Test
    void shouldMaskTheMessagesOfTheExceptions() {
        var exception = new SentryException();
        exception.setType("IllegalStateException");
        exception.setValue("Profil introuvable : /api/v1/public-profiles/alice-dupont");
        var event = new SentryEvent();
        event.setExceptions(List.of(exception));

        List<SentryException> sent =
                Objects.requireNonNull(scrubber.execute(event, new Hint()).getExceptions());

        assertThat(sent.getFirst().getValue()).isEqualTo("Profil introuvable : /api/v1/public-profiles/:handle");
    }

    @Test
    void shouldMaskTheBreadcrumbsLeftByTheLogsBeforeTheError() {
        var breadcrumb = new Breadcrumb();
        breadcrumb.setMessage("HTTP Request to " + URI + " failed");
        var event = new SentryEvent();
        event.setBreadcrumbs(List.of(breadcrumb));

        List<Breadcrumb> sent =
                Objects.requireNonNull(scrubber.execute(event, new Hint()).getBreadcrumbs());

        assertThat(sent.getFirst().getMessage()).isEqualTo("HTTP Request to " + MASKED_URI + " failed");
    }

    @Test
    void shouldKeepAnEventWithoutMessageNorException() {
        var event = new SentryEvent();

        assertThat(scrubber.execute(event, new Hint())).isSameAs(event);
    }
}
