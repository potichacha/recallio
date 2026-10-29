package com.recallio.content.domain.catalog;

import com.recallio.platform.domain.ResourceNotFoundException;

public class ThemeNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "content.theme-not-found";

    public ThemeNotFoundException(String slug) {
        super(CODE, "Aucun thème ne correspond à « " + slug + " ».");
    }
}
