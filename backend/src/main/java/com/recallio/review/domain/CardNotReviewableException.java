package com.recallio.review.domain;

import com.recallio.platform.domain.ResourceNotFoundException;
import java.util.UUID;

/** La carte n'existe pas ou a été retirée du catalogue. */
public class CardNotReviewableException extends ResourceNotFoundException {

    public static final String CODE = "review.card-not-found";

    public CardNotReviewableException(UUID cardId) {
        super(CODE, "Aucune carte active ne correspond à " + cardId + ".");
    }
}
