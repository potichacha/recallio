package com.recallio.platform.domain;

import java.util.List;
import java.util.function.Function;

/** Une page de résultats, avec le total pour la pagination côté client. */
public record Page<T>(List<T> items, int page, int size, long totalItems) {

    public Page {
        items = List.copyOf(items);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) ((totalItems + size - 1) / size);
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }
}
