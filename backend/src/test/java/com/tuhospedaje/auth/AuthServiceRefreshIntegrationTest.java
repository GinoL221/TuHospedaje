package com.tuhospedaje.auth;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.AuthService.AuthResult;
import com.tuhospedaje.service.RefreshSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers {@link AuthService#refresh(String)} (Design ADR-2, PR1/WU2) with refresh
 * sessions genuinely enabled — a separate context from {@link AuthServiceImplTest},
 * which deliberately runs with the flag off to prove the kill-switch.
 */
@SpringBootTest(properties = "app.session.refresh.enabled=true")
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceRefreshIntegrationTest {

    @Autowired
    private AuthService authService;

    @Test
    void loginIssuesARefreshCredentialAndRefreshMintsANewJwtRotatingIt() {
        authService.register(new RegisterRequest("Ada", "Lovelace", "ada-refresh@test.com", "123456"));
        AuthResult loginResult = authService.login(new LoginRequest("ada-refresh@test.com", "123456"));
        assertThat(loginResult.refreshCredential()).isNotBlank();

        AuthResult refreshResult = authService.refresh(loginResult.refreshCredential());

        // NOTE: a freshly minted JWT is NOT asserted to differ textually from the login
        // token — JwtService serializes iat/exp with second resolution, so two mints of
        // the same claims within the same wall-clock second are byte-identical HMAC
        // signatures. That is not a spec requirement; the spec only requires the opaque
        // refresh credential to rotate (Scenario: "Valid refresh rotates credential").
        assertThat(refreshResult.token()).isNotBlank();
        assertThat(refreshResult.body().getEmail()).isEqualTo("ada-refresh@test.com");
        assertThat(refreshResult.refreshCredential()).isNotBlank();
        assertThat(refreshResult.refreshCredential()).isNotEqualTo(loginResult.refreshCredential());

        // The rotated credential must itself be usable for a subsequent refresh (proves
        // Session.userId() correctly threads through the ordinary rotation branch, not
        // just issue()).
        AuthResult secondRefresh = authService.refresh(refreshResult.refreshCredential());
        assertThat(secondRefresh.body().getEmail()).isEqualTo("ada-refresh@test.com");
    }

    @Test
    void refreshRejectsAnUnknownCredentialWithoutDisclosingWhy() {
        assertThatThrownBy(() -> authService.refresh("rt1.unknown.does-not-exist"))
                .isInstanceOf(RefreshSessionService.Rejected.class);
    }

    // Regression for the CRITICAL finding confirmed during PR1/WU2 review: refresh()'s own
    // @Transactional had no noRollbackFor, so the family revocation rotate() writes on
    // detected reuse was physically discarded when refresh() (the owning transaction, since
    // AuthController.refresh() has no transactional boundary of its own) rolled back on the
    // propagating Rejected exception. A same-test-transaction assertion can't observe this —
    // Spring's test-managed rollback at teardown would mask the bug either way — so this test
    // forces real commit boundaries with TestTransaction to reproduce the exact production
    // transaction-ownership shape.
    @Test
    void reuseDetectedThroughRefreshEndpointPersistsFamilyRevocationDespiteTheRejection() {
        authService.register(new RegisterRequest("Grace", "Hopper", "grace-reuse@test.com", "123456"));
        AuthResult loginResult = authService.login(new LoginRequest("grace-reuse@test.com", "123456"));
        AuthResult firstRefresh = authService.refresh(loginResult.refreshCredential());
        // A second legitimate rotation moves the family past generation 1, so replaying the
        // ORIGINAL credential below is genuine reuse of a stale token — not a same-generation
        // double-submit, which the retry-grace window would tolerate instead of revoking.
        AuthResult secondRefresh = authService.refresh(firstRefresh.refreshCredential());

        TestTransaction.flagForCommit();
        TestTransaction.end();

        // No ambient test transaction here: this call's @Transactional genuinely owns its
        // physical transaction, exactly as it does in production.
        assertThatThrownBy(() -> authService.refresh(loginResult.refreshCredential()))
                .isInstanceOf(RefreshSessionService.Rejected.class);

        TestTransaction.start();

        // The latest, otherwise-still-valid credential must now be dead too. Before the fix,
        // this call succeeded instead of throwing, because the family revocation never
        // persisted (it was rolled back along with the propagating Rejected exception).
        assertThatThrownBy(() -> authService.refresh(secondRefresh.refreshCredential()))
                .isInstanceOf(RefreshSessionService.Rejected.class);
    }
}
