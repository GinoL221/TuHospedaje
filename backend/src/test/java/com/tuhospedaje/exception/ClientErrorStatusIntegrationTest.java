package com.tuhospedaje.exception;

import com.tuhospedaje.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A request the client got wrong must not look like a server failure. These cases
 * used to fall through to the catch-all handler and answer 500, which hid real
 * outages behind caller mistakes and made the frontend retry instead of telling
 * the user what to fix.
 *
 * <p>All of them use publicly permitted endpoints so the status under test is the
 * one produced by request handling, not by the security layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClientErrorStatusIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedJsonBodyReturns400() throws Exception {
        Cookie csrf = obtainCsrfCookie(mockMvc);

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unsupportedHttpMethodReturns405() throws Exception {
        Cookie csrf = obtainCsrfCookie(mockMvc);

        mockMvc.perform(put("/api/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        Cookie csrf = obtainCsrfCookie(mockMvc);

        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf)
                        .header("X-XSRF-TOKEN", csrf.getValue())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=user@example.com"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void missingRequiredRequestParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/lodgings/recommendations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
