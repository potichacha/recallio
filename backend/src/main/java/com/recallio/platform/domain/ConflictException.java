package com.recallio.platform.domain;

/** La requête contredit l'état actuel de la ressource (par exemple un identifiant de requête déjà utilisé) : 409. */
public class ConflictException extends DomainException {

    public ConflictException(String code, String detail) {
        super(code, detail);
    }
}
