package com.recallio.platform.domain;

/** Un service dont dépend la requête est indisponible pour l'instant ; la même requête pourra réussir plus tard : 503. */
public class UnavailableException extends DomainException {

    public UnavailableException(String code, String detail) {
        super(code, detail);
    }
}
