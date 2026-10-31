package com.recallio.content.domain.source;

import com.recallio.platform.domain.DomainException;

/** Le document ne peut pas être importé du tout (en-tête absent ou invalide) : traduite en 422. */
public class InvalidSourceException extends DomainException {

    public static final String CODE = "content.invalid-source";

    public InvalidSourceException(String detail) {
        super(CODE, detail);
    }
}
