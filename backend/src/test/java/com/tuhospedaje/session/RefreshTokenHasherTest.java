package com.tuhospedaje.session;

import com.tuhospedaje.configuration.SessionProperties;
import com.tuhospedaje.security.RefreshTokenHasher;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher hasher = new RefreshTokenHasher(
            new SessionProperties.KeyRingProperties("active", List.of(
                    new SessionProperties.KeyEntry("active", "active-test-pepper"),
                    new SessionProperties.KeyEntry("retired", "retired-test-pepper")
            )),
            new SecureRandom()
    );

    @Test
    void generatesVersionedActiveKeyCredentialWith256BitDigestOnly() {
        RefreshTokenHasher.GeneratedCredential credential = hasher.generate();

        assertThat(credential.presentedValue()).matches("rt1\\.active\\.[A-Za-z0-9_-]{43}");
        assertThat(credential.keyId()).isEqualTo("active");
        assertThat(credential.digest()).hasSize(32);
        assertThat(credential.digest()).isEqualTo(hasher.digest(credential.presentedValue()).digest());
    }

    @Test
    void routesRetiredKeyCredentialsToTheirConfiguredPepper() {
        String credential = "rt1.retired.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        RefreshTokenHasher.Digest retiredDigest = hasher.digest(credential);
        byte[] independentlyComputedRetiredDigest = hmac(credential, "retired-test-pepper");
        byte[] independentlyComputedActiveDigest = hmac(credential, "active-test-pepper");

        assertThat(retiredDigest.keyId()).isEqualTo("retired");
        assertThat(HexFormat.of().formatHex(retiredDigest.digest()))
                .isEqualTo("ca49d397025e55643b523a3dd55beac76ca699cf5a5618b84eb765d9153225bf");
        assertThat(retiredDigest.digest()).isEqualTo(independentlyComputedRetiredDigest);
        assertThat(HexFormat.of().formatHex(independentlyComputedActiveDigest))
                .isEqualTo("1fe38f2e3455b3469eaa391a9818bd4ce82debf66f34a6b754248ece4ae7445b");
        assertThat(independentlyComputedActiveDigest).isNotEqualTo(retiredDigest.digest());
    }

    private byte[] hmac(String credential, String pepper) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(credential.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate independent HMAC control", exception);
        }
    }
}
