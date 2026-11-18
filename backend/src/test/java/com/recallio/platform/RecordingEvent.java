package com.recallio.platform;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.NotificationOptions;
import jakarta.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Un {@link Event} CDI pour les tests unitaires : il garde les événements publiés au lieu de les remettre à des
 * observateurs. Seule la publication synchrone est prise en charge.
 */
public final class RecordingEvent<T> implements Event<T> {

    private final List<T> fired = new ArrayList<>();

    /** Les événements publiés, dans l'ordre. */
    public List<T> fired() {
        return List.copyOf(fired);
    }

    @Override
    public void fire(T event) {
        fired.add(event);
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> CompletionStage<U> fireAsync(U event, NotificationOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Event<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Event<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }
}
