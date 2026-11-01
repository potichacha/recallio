package com.recallio.platform.domain;

/**
 * L'appelant voit la ressource mais n'a pas le droit d'y faire cette action (par exemple un membre qui retire un autre
 * membre d'un groupe) : 403.
 */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String code, String detail) {
        super(code, detail);
    }
}
