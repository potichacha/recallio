package com.recallio.platform.domain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

/**
 * UUID v8 (RFC 9562) dérivé d'un nom : le même couple (espace, nom) donne toujours le même identifiant.
 *
 * <p>Sert aux identifiants qui doivent survivre aux réimports, comme ceux des cartes (ADR 0008 et 0009).
 */
public final class StableUuid {

    private static final int VERSION_8 = 0x80;
    private static final int VERSION_MASK = 0x0f;
    private static final int VARIANT_RFC_9562 = 0x80;
    private static final int VARIANT_MASK = 0x3f;
    private static final int UUID_BYTES = 16;

    private StableUuid() {}

    public static UUID of(String namespace, String name) {
        byte[] hash = sha256(namespace + '\u0000' + name);
        byte[] bytes = Arrays.copyOf(hash, UUID_BYTES);
        bytes[6] = (byte) ((bytes[6] & VERSION_MASK) | VERSION_8);
        bytes[8] = (byte) ((bytes[8] & VARIANT_MASK) | VARIANT_RFC_9562);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 est garanti par la plateforme Java", e);
        }
    }
}
