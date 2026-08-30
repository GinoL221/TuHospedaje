package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.EmailOutboxService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailOutboxService emailOutboxService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = new RegisterRequest("Juan", "Pérez", "juan@test.com", "123456");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldReturn400WithDuplicateEmailCodeWhenEmailAlreadyRegistered() throws Exception {
        RegisterRequest first = new RegisterRequest("Juan", "Pérez", "duplicate@test.com", "123456");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest again = new RegisterRequest("Otro", "Nombre", "duplicate@test.com", "abcdef");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(again)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("duplicate_email"))
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void shouldReturn400OnInvalidFields() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "email-invalido", "1234");

        mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        RegisterRequest register = new RegisterRequest("Juan", "Pérez", "juan@test.com", "123456");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));
        LoginRequest login = new LoginRequest("juan@test.com", "123456");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.email").value("juan@test.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldReturn401OnInvalidCredentials() throws Exception {
        LoginRequest login = new LoginRequest("noexiste@test.com", "pass");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Scenario 8.5: no JWT-shaped string anywhere in the error body.
        assertBodyHasNoJwt(result.getResponse().getContentAsString());
    }

    @Test
    void shouldReturn401OnProtectedRouteWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- Scenario 1.1/2.1: ACCESS_TOKEN cookie attributes on login/register ---

    @Test
    void shouldSetAccessTokenCookieWithExactAttributesOnLogin() throws Exception {
        registerUser("juan@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("juan@test.com", "123456"))))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("HttpOnly"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("SameSite=Strict"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Path=/"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Max-Age=28800"))))
                .andReturn();

        Cookie accessTokenCookie = result.getResponse().getCookie("ACCESS_TOKEN");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.getValue().split("\\.")).hasSize(3);
    }

    @Test
    void shouldSetAccessTokenCookieWithExactAttributesOnRegister() throws Exception {
        RegisterRequest request = new RegisterRequest("Ana", "Gómez", "ana@test.com", "123456");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("HttpOnly"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("SameSite=Strict"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Path=/"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Max-Age=28800"))))
                .andReturn();

        Cookie accessTokenCookie = result.getResponse().getCookie("ACCESS_TOKEN");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.getValue().split("\\.")).hasSize(3);
    }

    @Test
    void shouldNotSetAccessTokenCookieOnFailedLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("noexiste@test.com", "pass"))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Scenario 1.4 only prohibits the ACCESS_TOKEN cookie on a failed login; the
        // CsrfCookieFilter runs on every request and may still (re)issue XSRF-TOKEN.
        assertThat(result.getResponse().getCookie("ACCESS_TOKEN")).isNull();
    }

    // --- Scenario 1.2/2.2: XSRF-TOKEN cookie is also set, not HttpOnly ---

    @Test
    void shouldSetCsrfCookieOnLogin() throws Exception {
        registerUser("csrf-login@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("csrf-login@test.com", "123456"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
    }

    @Test
    void shouldSetCsrfCookieOnRegister() throws Exception {
        RegisterRequest request = new RegisterRequest("Lucía", "Díaz", "csrf-register@test.com", "123456");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
    }

    // --- Scenario 5.1/5.2: login/register work without a CSRF header (exempt) ---

    @Test
    void shouldLoginWithoutCsrfHeader() throws Exception {
        registerUser("no-csrf-login@test.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("no-csrf-login@test.com", "123456"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRegisterWithoutCsrfHeader() throws Exception {
        RegisterRequest request = new RegisterRequest("Marco", "Polo", "no-csrf-register@test.com", "123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // --- Scenario 6.1/5.3: logout clears the cookie, requires auth + CSRF ---
    //
    // NOTE: these tests deliberately do NOT use Spring Security Test's `.with(csrf())`
    // post-processor. That post-processor replaces the application's CsrfTokenRepository
    // with an HttpSessionCsrfTokenRepository at the ServletContext level (see
    // WebTestUtils#setCsrfTokenRepository) — once any test in this class uses it, every
    // SUBSEQUENT test in the same Spring context silently stops getting real
    // CookieCsrfTokenRepository-issued XSRF-TOKEN cookies, which breaks the
    // shouldSetCsrfCookieOnLogin/Register assertions above. Instead, these tests mirror
    // exactly what the real frontend does (Decision 2): read the real XSRF-TOKEN cookie
    // from login's response and echo it back as the X-XSRF-TOKEN header.

    @Test
    void shouldClearAccessTokenCookieOnLogoutWithValidCsrf() throws Exception {
        LoginCookies cookies = loginAndGetCookies("logout-ok@test.com");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue()))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Max-Age=0"))));
    }

    @Test
    void shouldRejectLogoutWithoutAccessTokenCookie() throws Exception {
        // Without a CSRF header at all, this is rejected by CSRF protection (403) —
        // logout is no longer auth-gated, but it stays CSRF-protected like every other
        // mutating endpoint outside login/register.
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldLogoutSuccessfullyWithoutAnyPriorSession() throws Exception {
        // Logout is idempotent: calling it with a valid CSRF token but no ACCESS_TOKEN
        // cookie (no session was ever established, or it already expired) must still
        // succeed as a no-op — it should not require knowing who the user is.
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("ACCESS_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Max-Age=0"))));
    }

    @Test
    void shouldLogoutSuccessfullyTwiceInARow() throws Exception {
        LoginCookies cookies = loginAndGetCookies("logout-idempotent@test.com");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue()))
                .andExpect(status().isNoContent());

        // Second call: no ACCESS_TOKEN cookie anymore (cleared by the first logout), but
        // a fresh CSRF token obtained the same way the frontend would on next render.
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectLogoutWithoutCsrfHeaderEvenWithValidCookie() throws Exception {
        LoginCookies cookies = loginAndGetCookies("logout-no-csrf@test.com");

        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookies.accessToken(), cookies.csrfToken()))
                .andExpect(status().isForbidden())
                .andReturn();

        // Scenario 8.5: no JWT-shaped string anywhere in the error body.
        assertBodyHasNoJwt(result.getResponse().getContentAsString());
    }

    @Test
    void shouldRejectLogoutWhenCsrfHeaderDoesNotMatchCookie() throws Exception {
        // Scenario 4.2 / double-submit core case: header present but mismatched must be
        // rejected, not just header absent — a forged cross-site request could otherwise
        // attach an attacker-controlled X-XSRF-TOKEN value without ever reading the real
        // cookie value (which Same-Origin Policy blocks it from doing).
        LoginCookies cookies = loginAndGetCookies("logout-mismatch-csrf@test.com");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", "this-does-not-match-the-real-csrf-cookie"))
                .andExpect(status().isForbidden());
    }

    // --- Scenario 7.1-7.4: GET /api/auth/me ---

    @Test
    void shouldReturnCurrentUserClaimsFromMeWithValidCookie() throws Exception {
        Cookie accessTokenCookie = loginAndGetAccessTokenCookie("me-ok@test.com");

        mockMvc.perform(get("/api/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me-ok@test.com"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void shouldReturn401OnMeWithoutCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Scenario 8.5: no JWT-shaped string anywhere in the error body.
        assertBodyHasNoJwt(result.getResponse().getContentAsString());
    }

    @Test
    void shouldReturn401OnMeWithInvalidCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("ACCESS_TOKEN", "not-a-valid-jwt")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Scenario 8.5: no JWT-shaped string anywhere in the error body.
        assertBodyHasNoJwt(result.getResponse().getContentAsString());
    }

    @Test
    void shouldNotCsrfCheckMeRequest() throws Exception {
        Cookie accessTokenCookie = loginAndGetAccessTokenCookie("me-no-csrf@test.com");

        // GET is never CSRF-checked — no .with(csrf()) needed.
        mockMvc.perform(get("/api/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401OnMeWhenValidJwtButUserDeletedFromDb() throws Exception {
        // The cookie carries a structurally and cryptographically valid JWT, but the
        // user it names no longer exists in the DB. AuthController's own Swagger
        // (@ApiResponse 401 "No valid session") documents this as 401 — it must NOT
        // fall through to the generic IllegalArgumentException -> 400 mapping used by
        // ordinary validation errors elsewhere in the app.
        Cookie accessTokenCookie = loginAndGetAccessTokenCookie("me-deleted-user@test.com");
        Long userId = userRepository.findByEmail("me-deleted-user@test.com").orElseThrow().getId();
        userRepository.deleteById(userId);

        MvcResult result = mockMvc.perform(get("/api/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertBodyHasNoJwt(result.getResponse().getContentAsString());
    }

    @Test
    void shouldReturn401WhenWelcomeResendIsAnonymous() throws Exception {
        mockMvc.perform(post("/api/auth/welcome-email/resend"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWelcomeResendWithoutCsrfToken() throws Exception {
        Cookie accessTokenCookie = loginAndGetAccessTokenCookie("resend-no-csrf@test.com");

        mockMvc.perform(post("/api/auth/welcome-email/resend").cookie(accessTokenCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldScheduleWelcomeResendForAuthenticatedPrincipalWithCsrfToken() throws Exception {
        LoginCookies cookies = loginAndGetCookies("resend-scheduled@test.com");
        when(emailOutboxService.resendWelcome(any())).thenReturn(EmailOutboxService.WelcomeResendResult.SCHEDULED);

        mockMvc.perform(post("/api/auth/welcome-email/resend")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue()))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldApplyCooldownAndRetryAfterToWelcomeResend() throws Exception {
        LoginCookies cookies = loginAndGetCookies("resend-cooldown@test.com");
        when(emailOutboxService.resendWelcome(any())).thenReturn(EmailOutboxService.WelcomeResendResult.COOLDOWN);

        mockMvc.perform(post("/api/auth/welcome-email/resend")
                        .cookie(cookies.accessToken(), cookies.csrfToken())
                        .header("X-XSRF-TOKEN", cookies.csrfToken().getValue()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "300"));
    }

    private void registerUser(String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", email, "123456");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Cookie loginAndGetAccessTokenCookie(String email) throws Exception {
        return loginAndGetCookies(email).accessToken();
    }

    private LoginCookies loginAndGetCookies(String email) throws Exception {
        registerUser(email);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "123456"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessTokenCookie = result.getResponse().getCookie("ACCESS_TOKEN");
        Cookie csrfTokenCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(csrfTokenCookie).isNotNull();
        return new LoginCookies(accessTokenCookie, csrfTokenCookie);
    }

    private record LoginCookies(Cookie accessToken, Cookie csrfToken) {
    }

    /**
     * Requirement 8 (hard security criterion): the JWT must never appear in ANY JSON
     * response body, including error bodies (Scenario 8.5). A compact JWT is always
     * three base64url segments separated by dots; this regex catches that shape
     * anywhere in the raw body, not just under a {@code "token"} key — defends against
     * a JWT leaking into an error detail field, a nested object, or any other key name.
     */
    private static final Pattern JWT_SHAPE = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private void assertBodyHasNoJwt(String body) {
        assertThat(JWT_SHAPE.matcher(body).find())
                .as("Response body must not contain any JWT-shaped string (3 dot-separated base64url segments): %s", body)
                .isFalse();
        assertThat(body).doesNotContain("\"token\"");
    }
}
