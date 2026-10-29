package com.recallio.user.domain;

/**
 * Ce que le jeton OIDC dit de l'utilisateur, indépendamment du fournisseur.
 *
 * @param subject le claim {@code sub}
 * @param suggestedName le nom proposé par le fournisseur ({@code name}, sinon {@code preferred_username}), peut être vide
 */
public record AuthenticatedUser(String subject, String suggestedName) {}
