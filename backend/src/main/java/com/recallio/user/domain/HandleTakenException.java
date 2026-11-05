package com.recallio.user.domain;

import com.recallio.platform.domain.ConflictException;

/** Un autre utilisateur a déjà cette adresse de profil public (ADR 0043). */
public class HandleTakenException extends ConflictException {

    public static final String CODE = "user.handle-taken";

    public HandleTakenException(PublicHandle handle) {
        super(CODE, "L'adresse « %s » est déjà prise : choisis-en une autre.".formatted(handle.value()));
    }
}
