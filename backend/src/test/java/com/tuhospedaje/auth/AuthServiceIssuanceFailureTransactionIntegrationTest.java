package com.tuhospedaje.auth;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.dto.auth.LoginRequest;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.security.RefreshTokenHasher;
import com.tuhospedaje.service.AuthService;
import com.tuhospedaje.service.AuthService.AuthResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Empirically settles whether AuthServiceImpl.issueRefreshCredential()'s try/catch actually
 * achieves graceful degradation through Spring's REAL transactional proxies (unlike
 * AuthServiceImplIssuanceFailureTest, which constructs AuthServiceImpl by hand with mocks and
 * therefore never exercises RefreshSessionServiceImpl's own @Transactional participation).
 *
 * RefreshTokenFamilyRepository is spied (real bean, real DB-backed behavior by default) so it
 * can be selectively made to throw for one specific save() call — RefreshSessionServiceImpl.
 * issue() still runs behind its real @Transactional AOP proxy, participating in register()/
 * login()'s transaction, exactly as in production.
 */
@SpringBootTest(properties = "app.session.refresh.enabled=true")
@Import(TestcontainersConfiguration.class)
class AuthServiceIssuanceFailureTransactionIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private RefreshTokenFamilyRepository families;

    @MockitoSpyBean
    private RefreshTokenHasher hasher;

    @Test
    void issuanceFailureBeforeAnyWriteLeavesNoOrphanedFamilyRow() {
        // Regression for the round-3 CRITICAL: hasher.generate() now runs BEFORE
        // families.save(family), so its realistic failure modes (misconfigured key,
        // Mac/JCE failure) can't leave a persisted family row with no matching token.
        doThrow(new RuntimeException("simulated key-ring misconfiguration")).when(hasher).generate();

        long familiesBefore = families.count();

        AuthResult result = authService.register(
                new RegisterRequest("Marie", "Curie", "marie-hasher-failure@test.com", "123456"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshCredential()).isNull();
        assertThat(families.count()).isEqualTo(familiesBefore);
    }

    @Test
    void registerStillPersistsTheUserAndReturnsAnAccessTokenWhenIssuanceFailsInsideTheSharedTransaction() {
        doThrow(new RuntimeException("simulated refresh_token_families insert failure")).when(families).save(any());

        AuthResult result = authService.register(
                new RegisterRequest("Ada", "Lovelace", "ada-tx-issuance-failure@test.com", "123456"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshCredential()).isNull();
        assertThat(userRepository.findByEmail("ada-tx-issuance-failure@test.com")).isPresent();
    }

    @Test
    void loginStillSucceedsWhenIssuanceFailsInsideTheSharedTransaction() {
        // Seed the user with issuance still working (spy behaves like the real repository
        // until stubbed below), then make it fail for the login call under test.
        authService.register(new RegisterRequest("Grace", "Hopper", "grace-tx-issuance-failure@test.com", "123456"));
        doThrow(new RuntimeException("simulated refresh_token_families insert failure")).when(families).save(any());

        AuthResult result = authService.login(new LoginRequest("grace-tx-issuance-failure@test.com", "123456"));

        assertThat(result.token()).isNotBlank();
        assertThat(result.refreshCredential()).isNull();
    }
}
