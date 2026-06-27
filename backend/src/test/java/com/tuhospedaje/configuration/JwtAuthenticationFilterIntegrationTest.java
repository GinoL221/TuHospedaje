package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.UserRepository;
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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Test
    void shouldReturnForbiddenInsteadOfServerErrorWhenTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/users")
                        .cookie(accessCookie("not-a-valid-jwt-token")))
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
                        .cookie(accessCookie(expiredToken)))
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
                        .cookie(accessCookie(badlySignedToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAuthenticateFromAccessTokenCookie() throws Exception {
        User admin = userRepository.save(User.builder()
                .firstName("Cookie")
                .lastName("Admin")
                .email("cookie-admin@test.com")
                .password("irrelevant")
                .role(RoleEnum.ADMIN)
                .build());
        String token = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/users")
                        .cookie(new jakarta.servlet.http.Cookie("ACCESS_TOKEN", token)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnForbiddenInsteadOfServerErrorWhenCookieTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/users")
                        .cookie(new jakarta.servlet.http.Cookie("ACCESS_TOKEN", "not-a-valid-jwt-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenInsteadOfServerErrorWhenCookieTokenIsExpired() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("expired-user@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 100_000))
                .expiration(new Date(System.currentTimeMillis() - 50_000))
                .signWith(signingKey())
                .compact();

        mockMvc.perform(get("/api/users")
                        .cookie(new jakarta.servlet.http.Cookie("ACCESS_TOKEN", expiredToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenInsteadOfServerErrorWhenCookieTokenHasInvalidSignature() throws Exception {
        SecretKey wrongKey = Jwts.SIG.HS256.key().build();
        String badlySignedToken = Jwts.builder()
                .subject("someone@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get("/api/users")
                        .cookie(new jakarta.servlet.http.Cookie("ACCESS_TOKEN", badlySignedToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAuthenticateViaCookieEvenWhenSpuriousAuthorizationHeaderIsPresent() throws Exception {
        User cookieAdmin = userRepository.save(User.builder()
                .firstName("Cookie")
                .lastName("Preferred")
                .email("cookie-preferred@test.com")
                .password("irrelevant")
                .role(RoleEnum.ADMIN)
                .build());
        String cookieToken = jwtService.generateToken(cookieAdmin);

        // Header carries a token for a user that does not exist — if the filter ever
        // read the header first, this would 403 (unknown user). The cookie token is for
        // a real, persisted ADMIN, so authentication must succeed.
        SecretKey wrongKey = Jwts.SIG.HS256.key().build();
        String unrelatedHeaderToken = Jwts.builder()
                .subject("does-not-exist@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(wrongKey)
                .compact();

        mockMvc.perform(get("/api/users")
                        .cookie(new jakarta.servlet.http.Cookie("ACCESS_TOKEN", cookieToken))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + unrelatedHeaderToken))
                .andExpect(status().isOk());
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
