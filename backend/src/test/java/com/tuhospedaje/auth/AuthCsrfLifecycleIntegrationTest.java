package com.tuhospedaje.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthCsrfLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticatedBootstrapReturnsFreshReadableCookieAndNoBody() throws Exception {
        Cookie accessToken = login("csrf-bootstrap@test.com");

        MvcResult result = mockMvc.perform(get("/api/auth/csrf").cookie(accessToken))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();

        Cookie csrfToken = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfToken).isNotNull();
        assertThat(csrfToken.isHttpOnly()).isFalse();
        assertThat(csrfToken.getPath()).isEqualTo("/");
    }

    @Test
    void bootstrapRequiresAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(result.getResponse().getCookie("XSRF-TOKEN")).isNull();
    }

    @Test
    void logoutAcceptsRawCookieTokenInHeaderAfterBootstrap() throws Exception {
        Cookie accessToken = login("csrf-logout@test.com");
        Cookie csrfToken = csrfToken(accessToken);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(accessToken, csrfToken)
                        .header("X-XSRF-TOKEN", csrfToken.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutRejectsMissingAndMismatchedTokens() throws Exception {
        Cookie accessToken = login("csrf-reject@test.com");
        Cookie csrfToken = csrfToken(accessToken);

        mockMvc.perform(post("/api/auth/logout").cookie(accessToken, csrfToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(accessToken, csrfToken)
                        .header("X-XSRF-TOKEN", "stale-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginRotatesAnyPreExistingCsrfCookieToAnImmediatelyUsableOne() throws Exception {
        Cookie preLoginToken = mockMvc.perform(get("/api/categories"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(preLoginToken).isNotNull();

        RegisterRequest request = new RegisterRequest("Test", "User", "csrf-fixation@test.com", "123456");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("csrf-fixation@test.com", "123456")))
                        .cookie(preLoginToken))
                .andExpect(status().isOk())
                .andReturn();
        Cookie accessToken = loginResult.getResponse().getCookie("ACCESS_TOKEN");

        // The response must never leave the client with only a cleared cookie: the fresh
        // token has to be immediately usable, without depending on a separate bootstrap call.
        Cookie rotatedToken = loginResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(rotatedToken).isNotNull();
        assertThat(rotatedToken.getValue()).isNotEqualTo(preLoginToken.getValue());
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(accessToken, rotatedToken)
                        .header("X-XSRF-TOKEN", rotatedToken.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void registerRotatesAnyPreExistingCsrfCookieToAnImmediatelyUsableOne() throws Exception {
        Cookie preRegisterToken = mockMvc.perform(get("/api/categories"))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(preRegisterToken).isNotNull();

        RegisterRequest request = new RegisterRequest("Test", "User", "csrf-fixation-register@test.com", "123456");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .cookie(preRegisterToken))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie accessToken = registerResult.getResponse().getCookie("ACCESS_TOKEN");

        // The response must never leave the client with only a cleared cookie: the fresh
        // token has to be immediately usable, without depending on a separate bootstrap call.
        Cookie rotatedToken = registerResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(rotatedToken).isNotNull();
        assertThat(rotatedToken.getValue()).isNotEqualTo(preRegisterToken.getValue());
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(accessToken, rotatedToken)
                        .header("X-XSRF-TOKEN", rotatedToken.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unrelatedAuthenticatedRequestDoesNotRotateCsrfCookie() throws Exception {
        Cookie accessToken = login("csrf-stable@test.com");
        Cookie csrfToken = csrfToken(accessToken);

        MvcResult result = mockMvc.perform(get("/api/categories").cookie(accessToken, csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getCookie("XSRF-TOKEN")).isNull();
    }

    private Cookie login(String email) throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", email, "123456");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "123456"))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("ACCESS_TOKEN");
    }

    private Cookie csrfToken(Cookie accessToken) throws Exception {
        return mockMvc.perform(get("/api/auth/csrf").cookie(accessToken))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }
}
