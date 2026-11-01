package com.recallio.group.domain;

import com.recallio.platform.domain.DomainException;

/** Une donnée de groupe invalide (nom vide...) : 422. */
public class InvalidGroupException extends DomainException {

    public InvalidGroupException(String code, String detail) {
        super(code, detail);
    }
}
