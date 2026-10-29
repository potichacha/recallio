package com.recallio.platform.domain;

/**
 * Exception métier explicite. Le code est stable et fait partie du contrat de l'API : il est renvoyé au client dans
 * la réponse Problem Details (RFC 9457).
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final String detail;

    protected DomainException(String code, String detail) {
        super(detail);
        this.code = code;
        this.detail = detail;
    }

    public String code() {
        return code;
    }

    /** Message lisible par un humain, renvoyé dans le champ {@code detail} de la réponse. */
    public String detail() {
        return detail;
    }
}
