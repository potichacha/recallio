package com.recallio.notification.infrastructure.push;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebPushEncryptionTest {

    // L'exemple de la RFC 8291, section 5.
    private static final String PLAINTEXT = "When I grow up, I want to be a watermelon";
    private static final String AS_PUBLIC =
            "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String AS_PRIVATE = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
    private static final String UA_PUBLIC =
            "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String UA_PRIVATE = "q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94";
    private static final String AUTH_SECRET = "BTBZMqHH6r4Tts7J_aSIgg";
    private static final String SALT = "DGv6ra1nlYgDCS1FRnbzlw";
    private static final String ENCRYPTED =
            "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

    @Test
    void shouldReproduceTheExampleOfRfc8291() {
        byte[] body = WebPushEncryption.encrypt(
                PLAINTEXT.getBytes(StandardCharsets.UTF_8),
                EcKeys.publicKey(EcKeys.decode(UA_PUBLIC)),
                EcKeys.decode(AUTH_SECRET),
                EcKeys.pair(AS_PUBLIC, AS_PRIVATE),
                EcKeys.decode(SALT));

        assertThat(EcKeys.base64Url(body)).isEqualTo(ENCRYPTED);
    }

    @Test
    void shouldBeReadableOnlyByTheSubscribedDevice() throws Exception {
        KeyPair device = EcKeys.generate();
        byte[] auth = new byte[16];
        Arrays.fill(auth, (byte) 7);

        byte[] body = WebPushEncryption.encrypt(
                "{\"title\":\"Ta série t'attend\"}".getBytes(StandardCharsets.UTF_8),
                (ECPublicKey) device.getPublic(),
                auth,
                EcKeys.generate(),
                new byte[WebPushEncryption.SALT_LENGTH]);

        assertThat(new String(decrypt(body, device, auth), StandardCharsets.UTF_8))
                .isEqualTo("{\"title\":\"Ta série t'attend\"}");
        assertThat(ByteBuffer.wrap(body, WebPushEncryption.SALT_LENGTH, 4).getInt())
                .isEqualTo(WebPushEncryption.RECORD_SIZE);
    }

    @Test
    void shouldSignTheVapidTokenForTheOriginOfThePushService() throws Exception {
        KeyPair server = EcKeys.generate();

        String header = Vapid.authorization(
                "https://fcm.googleapis.com/fcm/send/abc", server, "mailto:admin@localhost", Instant.EPOCH);

        assertThat(header)
                .startsWith("vapid t=")
                .contains(", k=" + EcKeys.base64Url(EcKeys.encode((ECPublicKey) server.getPublic())));
        String token = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = token.split("\\.");
        String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertThat(claims)
                .isEqualTo("{\"aud\":\"https://fcm.googleapis.com\",\"exp\":43200,\"sub\":\"mailto:admin@localhost\"}");
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(server.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
    }

    @Test
    void shouldReadAndWriteRawP256Keys() {
        KeyPair pair = EcKeys.pair(UA_PUBLIC, UA_PRIVATE);

        assertThat(EcKeys.base64Url(EcKeys.encode((ECPublicKey) pair.getPublic())))
                .isEqualTo(UA_PUBLIC);
        assertThat(EcKeys.base64Url(EcKeys.encode((ECPrivateKey) pair.getPrivate())))
                .isEqualTo(UA_PRIVATE);
    }

    /** Ce que fait le navigateur : RFC 8291, côté appareil. */
    private static byte[] decrypt(byte[] body, KeyPair device, byte[] auth) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(body);
        byte[] salt = new byte[WebPushEncryption.SALT_LENGTH];
        buffer.get(salt);
        buffer.getInt();
        byte[] senderPublic = new byte[buffer.get()];
        buffer.get(senderPublic);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(device.getPrivate());
        agreement.doPhase(EcKeys.publicKey(senderPublic), true);
        byte[] uaPublic = EcKeys.encode((ECPublicKey) device.getPublic());
        byte[] prkKey = WebPushEncryption.hmac(auth, agreement.generateSecret());
        byte[] ikm = WebPushEncryption.hmac(
                prkKey,
                WebPushEncryption.concat(
                        "WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, senderPublic, new byte[] {1}));
        byte[] prk = WebPushEncryption.hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(
                WebPushEncryption.hmac(
                        prk,
                        WebPushEncryption.concat(
                                "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), new byte[] {1})),
                16);
        byte[] nonce = Arrays.copyOf(
                WebPushEncryption.hmac(
                        prk,
                        WebPushEncryption.concat(
                                "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), new byte[] {1})),
                12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] record = cipher.doFinal(ciphertext);
        assertThat(record[record.length - 1]).isEqualTo((byte) 2);
        return Arrays.copyOf(record, record.length - 1);
    }
}
