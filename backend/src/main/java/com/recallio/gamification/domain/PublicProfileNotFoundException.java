package com.recallio.gamification.domain;

import com.recallio.platform.domain.ResourceNotFoundException;

/**
 * Personne n'a cette adresse de profil, ou son profil est privé (ADR 0043) : les deux cas répondent de la même façon,
 * pour ne pas révéler qu'un profil privé existe.
 */
public class PublicProfileNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "gamification.public-profile-not-found";

    public PublicProfileNotFoundException(String handle) {
        super(CODE, "Aucun profil public à l'adresse « %s ».".formatted(handle));
    }
}
