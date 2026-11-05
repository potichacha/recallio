package com.recallio.notification.infrastructure.push;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Le chiffrement d'un message Web Push (RFC 8291) au format {@code aes128gcm} (RFC 8188), en un seul enregistrement :
 * seul l'appareil abonné peut le lire, pas le service de notification qui le transporte. Les noms suivent ceux des RFC.
 */
final class WebPushEncryption {

    /** La taille d'enregistrement annoncée dans l'en-tête : un message de rappel tient dans un seul. */
    static final int RECORD_SIZE = 4096;

    static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 16;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final byte LAST_RECORD = 0x02;

    private static final byte[] KEY_INFO = "WebPush: info\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] CEK_INFO = "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] NONCE_INFO = "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII);

    private WebPushEncryption() {}

    /**
     * @param userAgentKey la clé publique de l'appareil ({@code p256dh})
     * @param authSecret son secret d'authentification ({@code auth}), 16 octets
     * @param senderKeys une paire de clés éphémère du serveur, neuve pour chaque message
     * @param salt 16 octets aléatoires, neufs pour chaque message
     * @return le corps de la requête : l'en-tête aes128gcm, puis le texte chiffré
     */
    static byte[] encrypt(
            byte[] plaintext, ECPublicKey userAgentKey, byte[] authSecret, KeyPair senderKeys, byte[] salt) {
        try {
            byte[] uaPublic = EcKeys.encode(userAgentKey);
            byte[] asPublic = EcKeys.encode((ECPublicKey) senderKeys.getPublic());
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(senderKeys.getPrivate());
            agreement.doPhase(userAgentKey, true);
            byte[] ecdhSecret = agreement.generateSecret();

            // RFC 8291, section 3.4 : la clé d'entrée, liée aux deux clés publiques et au secret de l'appareil.
            byte[] prkKey = hmac(authSecret, ecdhSecret);
            byte[] ikm = hmac(prkKey, concat(KEY_INFO, uaPublic, asPublic, new byte[] {1}));
            // RFC 8188, section 2.2 : la clé de contenu et le nonce, tirés du sel.
            byte[] prk = hmac(salt, ikm);
            byte[] cek = Arrays.copyOf(hmac(prk, concat(CEK_INFO, new byte[] {1})), KEY_LENGTH);
            byte[] nonce = Arrays.copyOf(hmac(prk, concat(NONCE_INFO, new byte[] {1})), NONCE_LENGTH);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[] {LAST_RECORD}));

            return ByteBuffer.allocate(SALT_LENGTH + Integer.BYTES + 1 + asPublic.length + ciphertext.length)
                    .put(salt)
                    .putInt(RECORD_SIZE)
                    .put((byte) asPublic.length)
                    .put(asPublic)
                    .put(ciphertext)
                    .array();
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Chiffrement Web Push impossible", failure);
        }
    }

    static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
