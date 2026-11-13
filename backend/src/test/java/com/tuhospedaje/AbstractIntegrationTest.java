package com.tuhospedaje;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Clears every table that holds a foreign key to {@code users} BEFORE any subclass
     * {@code @BeforeEach} runs (JUnit runs superclass {@code @BeforeEach} methods
     * first), so a subclass's {@code userRepository.deleteAll()} can never collide with
     * foreign-key children left behind by a previously run test class.
     *
     * <p>The offender is {@code email_outbox}: {@code EmailOutboxServiceImpl.enqueueWelcome}
     * writes the welcome row with plain {@code REQUIRED} propagation, so the row commits
     * whenever the surrounding transaction commits. Plain {@code @SpringBootTest} classes
     * with no test transaction (e.g. {@code WelcomeResendHttpIntegrationTest}) therefore
     * leave committed outbox rows referencing their users, and the alphabetically later
     * rating/reservation classes collide with those rows when they delete users. Session
     * rows written by {@code RefreshSessionServiceImpl} (also {@code REQUIRED}) are the
     * same kind of leftover. {@code REQUIRES_NEW} exists only on the claim and outcome
     * writes in {@code EmailOutboxTransactionService} and is not part of the problem.
     *
     * <p>Child tables, per the migrations (FK → referenced table):
     * <ul>
     *   <li>{@code email_outbox} → users (V4__email_outbox.sql)</li>
     *   <li>{@code session_security_events} → users, refresh_token_families (V2)</li>
     *   <li>{@code refresh_tokens} → refresh_token_families, and self-references
     *       {@code predecessor_token_id} with no cascade (V2__refresh_session_families.sql:45)</li>
     *   <li>{@code refresh_token_families} → users (V2)</li>
     *   <li>{@code ratings} → users, lodgings (V1)</li>
     *   <li>{@code reservations} → users, lodgings (V1)</li>
     *   <li>{@code user_favorites} → users, lodgings (V1)</li>
     * </ul>
     *
     * <p>Deletion order respects the child-to-parent chain: session children before
     * their families. This does NOT truncate the whole schema — categories, lodgings,
     * features and policies stay untouched, so each test still sets up its own data.
     * Subclasses keep their own {@code @BeforeEach} cleanup; this only guarantees the
     * user children are gone first. Tests that cannot extend this class (e.g.
     * {@code ListingQueryCountTest}, whose measurement forbids the class-level
     * {@code @Transactional}) reuse {@link #clearForeignKeyChildrenOfUsers(JdbcTemplate)}
     * directly.
     */
    @BeforeEach
    void clearForeignKeyChildrenOfUsers() {
        clearForeignKeyChildrenOfUsers(jdbcTemplate);
    }

    /**
     * Shared cleanup used by the inherited {@code @BeforeEach} above and, directly,
     * by tests that cannot extend this class. Must be called before a test deletes
     * users.
     */
    public static void clearForeignKeyChildrenOfUsers(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM email_outbox");
        jdbcTemplate.update("DELETE FROM session_security_events");
        // refresh_tokens references itself (predecessor_token_id, no cascade), so delete
        // in id DESC order: MySQL removes rows in that order, children before predecessors.
        jdbcTemplate.update("DELETE FROM refresh_tokens ORDER BY id DESC");
        jdbcTemplate.update("DELETE FROM refresh_token_families");
        jdbcTemplate.update("DELETE FROM ratings");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM user_favorites");
    }

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
