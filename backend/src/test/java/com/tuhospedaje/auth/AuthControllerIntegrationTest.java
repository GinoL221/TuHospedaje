package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
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
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectLogoutWithoutCsrfHeaderEvenWithValidCookie() throws Exception {
        LoginCookies cookies = loginAndGetCookies("logout-no-csrf@test.com");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookies.accessToken(), cookies.csrfToken()))
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
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnMeWithInvalidCookie() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie("ACCESS_TOKEN", "not-a-valid-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotCsrfCheckMeRequest() throws Exception {
        Cookie accessTokenCookie = loginAndGetAccessTokenCookie("me-no-csrf@test.com");

        // GET is never CSRF-checked — no .with(csrf()) needed.
        mockMvc.perform(get("/api/auth/me").cookie(accessTokenCookie))
                .andExpect(status().isOk());
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
}
