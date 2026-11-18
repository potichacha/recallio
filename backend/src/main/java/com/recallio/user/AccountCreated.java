package com.recallio.user;

import java.time.Instant;

/**
 * Événement public du module user (ADR 0064) : un profil vient d'être créé, à la première connexion. Une connexion
 * suivante, ou une première connexion simultanée dans un deuxième onglet, ne le publie pas. Il est publié dans la
 * transaction qui crée le profil.
 */
public record AccountCreated(UserRef user, Instant createdAt) {}
