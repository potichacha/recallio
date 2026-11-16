package com.recallio.review.domain.playground;

import com.recallio.platform.domain.DomainException;

/**
 * Les fichiers envoyés ne forment pas un programme du Tour (ADR 0037) : deux langages, un fichier de tests, pas de
 * {@code Main.java} en Java, ou pas de {@code main.py} en Python.
 */
public class InvalidProgramException extends DomainException {

    public static final String CODE = "review.invalid-program";

    public InvalidProgramException(String detail) {
        super(CODE, detail);
    }
}
