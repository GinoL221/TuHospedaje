package com.tuhospedaje;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class AbstractIntegrationTest {

    /**
     * Obtains a real {@code XSRF-TOKEN} cookie to satisfy CSRF protection on mutating
     * requests, WITHOUT using Spring Security Test's {@code .with(csrf())}
     * post-processor. That post-processor replaces the application's
     * {@code CsrfTokenRepository} with a session-based one at the ServletContext level
     * (see {@code WebTestUtils#setCsrfTokenRepository}), which silently corrupts every
     * OTHER test in the same Spring context/Surefire fork that relies on the real
     * {@code CookieCsrfTokenRepository} (e.g. {@code AuthControllerIntegrationTest}).
     *
     * <p>Instead, this performs a real GET to a CSRF-safe, publicly accessible endpoint
     * — {@code CsrfCookieFilter} materializes the token on every response regardless of
     * authentication, so no login is required. CSRF validation needs BOTH the cookie
     * attached to the request AND a matching {@code X-XSRF-TOKEN} header, exactly like
     * the real frontend does (Design Decision 2):
     *
     * <pre>{@code
     * Cookie csrfCookie = obtainCsrfCookie(mockMvc);
     * mockMvc.perform(post(...)
     *         .cookie(csrfCookie)
     *         .header("X-XSRF-TOKEN", csrfCookie.getValue()));
     * }</pre>
     */
    protected Cookie obtainCsrfCookie(MockMvc mockMvc) throws Exception {
        Cookie csrfCookie = mockMvc.perform(get("/api/categories"))
                .andReturn()
                .getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        return csrfCookie;
    }

    /**
     * Builds the {@code ACCESS_TOKEN} cookie used to authenticate {@code MockMvc}
     * requests, mirroring how {@code JwtAuthenticationFilter} reads the JWT in
     * production (cookie-first). This is a plain in-memory wrapper — unlike
     * {@link #obtainCsrfCookie(MockMvc)}, it performs no real request.
     */
    protected Cookie accessCookie(String token) {
        return new Cookie("ACCESS_TOKEN", token);
    }
}
