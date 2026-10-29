package com.recallio.user.domain;

import com.recallio.platform.domain.DomainException;

public class InvalidProfileException extends DomainException {

    public InvalidProfileException(String code, String detail) {
        super(code, detail);
    }
}
