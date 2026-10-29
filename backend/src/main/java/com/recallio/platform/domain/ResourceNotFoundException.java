package com.recallio.platform.domain;

/** La ressource demandée n'existe pas (ou n'est pas visible par l'appelant) : traduite en 404. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String code, String message) {
        super(code, message);
    }
}
