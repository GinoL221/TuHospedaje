package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Test
    void shouldReturnForbiddenInsteadOfServerErrorWhenTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-jwt-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenInsteadOfServerErrorWhenTokenIsExpired() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("expired-user@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 100_000))
                .expiration(new Date(System.currentTimeMillis() - 50_000))
                .signWith(signingKey())
                .compact();

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenInsteadOfServerErrorWhenTokenHasInvalidSignature() throws Exception {
        SecretKey wrongKey = Jwts.SIG.HS256.key().build();
        String badlySignedToken = Jwts.builder()
                .subject("someone@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + badlySignedToken))
                .andExpect(status().isForbidden());
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
