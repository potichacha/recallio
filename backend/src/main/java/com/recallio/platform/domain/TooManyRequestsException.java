package com.recallio.platform.domain;

/** L'utilisateur a déjà une requête du même genre en cours, ou en envoie trop : 429. */
public class TooManyRequestsException extends DomainException {

    public TooManyRequestsException(String code, String detail) {
        super(code, detail);
    }
}
