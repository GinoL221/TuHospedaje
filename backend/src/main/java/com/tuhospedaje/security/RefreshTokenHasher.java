package com.tuhospedaje.security;

import com.tuhospedaje.configuration.SessionProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public final class RefreshTokenHasher {
    private static final String VERSION = "rt1";
    private final String activeKeyId;
    private final Map<String, String> keys;
    private final SecureRandom random;

    public RefreshTokenHasher(SessionProperties.KeyRingProperties keyRing, SecureRandom random) {
        this.activeKeyId = keyRing.activeKeyId();
        this.keys = keyRing.keys();
        this.random = random;
    }

    public GeneratedCredential generate() {
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String value = VERSION + "." + activeKeyId + "." + Base64.getUrlEncoder()
                .withoutPadding().encodeToString(randomBytes);
        Digest digest = digest(value);
        return new GeneratedCredential(value, digest.digest(), digest.keyId());
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public GeneratedCredential deriveSuccessor(String predecessor, UUID familyUuid, long generation, String keyId) {
        String secret = keys.get(keyId);
        if (secret == null) {
            throw new IllegalArgumentException("Unknown refresh credential key");
        }
        byte[] derived = hmac(secret, "refresh-successor-v1\0" + predecessor + "\0" + familyUuid + "\0" + generation);
        String value = VERSION + "." + keyId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
        Digest digest = digest(value);
        return new GeneratedCredential(value, digest.digest(), digest.keyId());
    }

    public Digest digest(String presentedValue) {
        String[] parts = presentedValue.split("\\.", -1);
        if (parts.length != 3 || !VERSION.equals(parts[0]) || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("Invalid refresh credential");
        }
        String secret = keys.get(parts[1]);
        if (secret == null) {
            throw new IllegalArgumentException("Unknown refresh credential key");
        }
        return new Digest(parts[1], hmac(secret, presentedValue));
    }

    private byte[] hmac(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to digest refresh credential", exception);
        }
    }

    public record GeneratedCredential(String presentedValue, byte[] digest, String keyId) {
    }

    public record Digest(String keyId, byte[] digest) {
    }
}
