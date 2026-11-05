package com.recallio.notification.infrastructure.push;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Les clés P-256 du Web Push (RFC 8291, 8292), sous leur forme brute : une clé publique est un point non compressé de
 * 65 octets (0x04, X, Y), une clé privée un entier de 32 octets ; en base64url sans remplissage.
 */
final class EcKeys {

    static final String CURVE = "secp256r1";
    static final int COORDINATE_LENGTH = 32;
    private static final byte UNCOMPRESSED = 0x04;
    private static final ECParameterSpec P256 = parameters();

    private EcKeys() {}

    static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec(CURVE));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("P-256 indisponible", impossible);
        }
    }

    static KeyPair pair(String publicKey, String privateKey) {
        return new KeyPair(publicKey(decode(publicKey)), privateKey(decode(privateKey)));
    }

    static ECPublicKey publicKey(byte[] uncompressed) {
        if (uncompressed.length != 1 + 2 * COORDINATE_LENGTH || uncompressed[0] != UNCOMPRESSED) {
            throw new IllegalArgumentException("Une clé publique P-256 brute fait 65 octets et commence par 0x04");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 1 + COORDINATE_LENGTH));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1 + COORDINATE_LENGTH, uncompressed.length));
        try {
            return (ECPublicKey)
                    KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
        } catch (GeneralSecurityException invalid) {
            throw new IllegalArgumentException("Clé publique P-256 invalide", invalid);
        }
    }

    static ECPrivateKey privateKey(byte[] raw) {
        try {
            return (ECPrivateKey)
                    KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, raw), P256));
        } catch (GeneralSecurityException invalid) {
            throw new IllegalArgumentException("Clé privée P-256 invalide", invalid);
        }
    }

    static byte[] encode(ECPublicKey key) {
        byte[] encoded = new byte[1 + 2 * COORDINATE_LENGTH];
        encoded[0] = UNCOMPRESSED;
        copyUnsigned(key.getW().getAffineX(), encoded, 1);
        copyUnsigned(key.getW().getAffineY(), encoded, 1 + COORDINATE_LENGTH);
        return encoded;
    }

    static byte[] encode(ECPrivateKey key) {
        byte[] encoded = new byte[COORDINATE_LENGTH];
        copyUnsigned(key.getS(), encoded, 0);
        return encoded;
    }

    static byte[] decode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }

    static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Un entier non signé sur 32 octets, complété à gauche : BigInteger peut en donner 31 ou 33. */
    private static void copyUnsigned(BigInteger value, byte[] target, int offset) {
        byte[] bytes = value.toByteArray();
        int length = Math.min(bytes.length, COORDINATE_LENGTH);
        System.arraycopy(bytes, bytes.length - length, target, offset + COORDINATE_LENGTH - length, length);
    }

    private static ECParameterSpec parameters() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec(CURVE));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException impossible) {
            throw new IllegalStateException("P-256 indisponible", impossible);
        }
    }
}
