package com.recallio.notification.application;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

/** Réglages des rappels ({@code recallio.push.*}, ADR 0047). Les clés VAPID sont des secrets : variables d'environnement. */
@ConfigMapping(prefix = "recallio.push")
public interface PushConfig {

    /** La clé publique VAPID, P-256 brute en base64url ; absente, une paire éphémère est tirée au démarrage. */
    Optional<String> vapidPublicKey();

    Optional<String> vapidPrivateKey();

    /** Le contact du serveur pour les services de notification (RFC 8292), une adresse mailto: ou https:. */
    @WithDefault("mailto:admin@localhost")
    String subject();

    /** Faux, aucun rappel ne part (les tests appellent l'envoi eux-mêmes). */
    @WithDefault("true")
    boolean remindersEnabled();
}
