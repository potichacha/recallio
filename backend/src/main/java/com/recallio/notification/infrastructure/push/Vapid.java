package com.recallio.notification.infrastructure.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * L'identification du serveur auprès d'un service de notification (VAPID, RFC 8292) : un jeton JWT signé en ES256,
 * valable quelques heures, pour l'origine de l'abonnement.
 */
final class Vapid {

    /** La durée de validité du jeton : 24 heures au plus selon la RFC. */
    static final Duration VALIDITY = Duration.ofHours(12);

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String HEADER =
            EcKeys.base64Url("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));

    private Vapid() {}

    /** L'en-tête {@code Authorization} d'une requête vers cet abonnement. */
    static String authorization(String endpoint, KeyPair keys, String subject, Instant now) {
        URI uri = URI.create(endpoint);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("aud", uri.getScheme() + "://" + uri.getAuthority());
        claims.put("exp", now.plus(VALIDITY).getEpochSecond());
        claims.put("sub", subject);
        try {
            String signed = HEADER + "." + EcKeys.base64Url(JSON.writeValueAsBytes(claims));
            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(keys.getPrivate());
            signature.update(signed.getBytes(StandardCharsets.US_ASCII));
            String token = signed + "." + EcKeys.base64Url(signature.sign());
            return "vapid t=" + token + ", k=" + EcKeys.base64Url(EcKeys.encode((ECPublicKey) keys.getPublic()));
        } catch (GeneralSecurityException | JsonProcessingException failure) {
            throw new IllegalStateException("Jeton VAPID impossible", failure);
        }
    }
}
