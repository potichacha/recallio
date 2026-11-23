package com.recallio.platform.infrastructure;

import io.quarkus.arc.Unremovable;
import io.sentry.Breadcrumb;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Message;
import io.sentry.protocol.SentryException;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Le dernier passage d'une erreur avant Sentry (ADR 0070) : son message, ses paramètres, les messages de ses exceptions
 * et ceux de ses fils d'Ariane (les logs qui l'ont précédée) perdent le jeton d'une invitation, l'adresse d'un profil
 * public et toute chaîne de requête ({@link SensitivePaths}).
 *
 * <p>L'extension Sentry appelle chaque bean {@link SentryOptions.BeforeSendCallback} ; aucun code ne l'injecte, d'où
 * {@link Unremovable}.
 */
@ApplicationScoped
@Unremovable
public class SentryEventScrubber implements SentryOptions.BeforeSendCallback {

    @Override
    public SentryEvent execute(SentryEvent event, Hint hint) {
        Message message = event.getMessage();
        if (message != null) {
            message.setMessage(mask(message.getMessage()));
            message.setFormatted(mask(message.getFormatted()));
            List<String> params = message.getParams();
            if (params != null) {
                message.setParams(params.stream().map(SentryEventScrubber::mask).toList());
            }
        }
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions != null) {
            exceptions.forEach(exception -> exception.setValue(mask(exception.getValue())));
        }
        List<Breadcrumb> breadcrumbs = event.getBreadcrumbs();
        if (breadcrumbs != null) {
            breadcrumbs.forEach(breadcrumb -> breadcrumb.setMessage(mask(breadcrumb.getMessage())));
        }
        return event;
    }

    private static @Nullable String mask(@Nullable String text) {
        return text == null ? null : SensitivePaths.mask(text);
    }
}
