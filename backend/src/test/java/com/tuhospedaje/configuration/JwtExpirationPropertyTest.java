package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SC-5.4: app.jwt.expiration drives token TTL (not a hardcoded constant).
 * SC-5.5: Default expiration in the dev/default profile is 28800000 ms (8 hours).
 */
@SpringBootTest
class JwtExpirationPropertyTest extends AbstractIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration}")
    private long expirationMillis;

    private User buildTestUser() {
        return User.builder()
                .id(99L)
                .firstName("Test")
                .lastName("User")
                .email("expiry-test@test.com")
                .password("irrelevant")
                .role(RoleEnum.USER)
                .build();
    }

    @Test
    void tokenExpirationMatchesConfiguredProperty() {
        long beforeIssue = System.currentTimeMillis();
        String token = jwtService.generateToken(buildTestUser());
        long afterIssue = System.currentTimeMillis();

        Claims claims = parseToken(token);

        long issuedAt = claims.getIssuedAt().getTime();
        long expiration = claims.getExpiration().getTime();
        long actualTtl = expiration - issuedAt;

        // JWT's NumericDate (RFC 7519) truncates iat/exp to whole seconds, so issuedAt can
        // legitimately land up to 999ms before beforeIssue — floor beforeIssue to the second
        // it belongs to rather than comparing millisecond-for-millisecond.
        long beforeIssueSecond = (beforeIssue / 1000) * 1000;
        assertThat(issuedAt)
                .as("Token iat must reflect real generation time, not a frozen/hardcoded value")
                .isBetween(beforeIssueSecond, afterIssue);

        assertThat(actualTtl)
                .as("Token TTL must equal app.jwt.expiration (%d ms)", expirationMillis)
                .isEqualTo(expirationMillis);
    }

    @Test
    void defaultExpirationIs8Hours() {
        // SC-5.5: default expiration must be 28800000 ms (8 hours)
        assertThat(expirationMillis)
                .as("Default app.jwt.expiration must be 28800000 (8 hours)")
                .isEqualTo(28_800_000L);
    }

    private Claims parseToken(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
