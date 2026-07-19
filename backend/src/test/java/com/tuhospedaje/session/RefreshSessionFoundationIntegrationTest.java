package com.tuhospedaje.session;

import com.tuhospedaje.configuration.SessionProperties;
import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.entity.RefreshToken;
import com.tuhospedaje.entity.RefreshTokenFamily;
import com.tuhospedaje.entity.SessionSecurityEvent;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.RefreshTokenFamilyRepository;
import com.tuhospedaje.repository.RefreshTokenRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RefreshSessionFoundationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionProperties sessionProperties;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenFamilyRepository familyRepository;

    @Autowired
    private RefreshTokenRepository tokenRepository;

    @Test
    void migratesRefreshSessionSchemaWithConstraintsIndexesAndUtcMicrosecondColumns() {
        assertThat(tableNames()).contains(
                "refresh_token_families",
                "refresh_tokens",
                "session_security_events"
        );
        assertThat(columnType("refresh_token_families", "issued_at")).isEqualTo("datetime(6)");
        assertThat(columnType("refresh_tokens", "expires_at")).isEqualTo("datetime(6)");
        assertThat(columnType("session_security_events", "occurred_at")).isEqualTo("datetime(6)");
        assertThat(indexNames("refresh_token_families")).contains("UK_refresh_token_families_family_uuid", "IX_refresh_token_families_user_revoked_expiry");
        assertThat(indexNames("refresh_tokens")).contains(
                "UK_refresh_tokens_hmac",
                "UK_refresh_tokens_family_generation",
                "UK_refresh_tokens_predecessor"
        );
        assertThat(indexNames("session_security_events")).contains("UK_session_security_events_family_type");
        assertThat(foreignKeyCount("refresh_tokens")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void preservesV1UserRowsAndBackfillsEnabledToTrue() {
        jdbcTemplate.update("""
                INSERT INTO users (email, first_name, last_name, password, role)
                VALUES ('v1-row@example.test', 'V1', 'User', 'bcrypt-placeholder', 'USER')
                """);

        Boolean enabled = jdbcTemplate.queryForObject(
                "SELECT enabled FROM users WHERE email = 'v1-row@example.test'", Boolean.class);

        assertThat(enabled).isTrue();
    }

    @Test
    void repositoryContractsLockFamiliesAndRevokeOnlyActiveFamiliesForOneUser() throws NoSuchMethodException {
        Lock lock = RefreshTokenFamilyRepository.class.getMethod("findByIdForUpdate", Long.class)
                .getAnnotation(Lock.class);
        Query revocation = RefreshTokenFamilyRepository.class.getMethod(
                        "revokeActiveFamiliesForUser", Long.class, java.time.Instant.class, String.class)
                .getAnnotation(Query.class);

        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(revocation.value()).contains("family.user.id = :userId", "family.revokedAt IS NULL");
    }

    @Test
    void revokingFamilyFlushesPendingChangesAndClearsManagedTokens() {
        Instant issuedAt = Instant.parse("2026-07-18T12:00:00Z");
        Instant lastPresentedAt = issuedAt.plusSeconds(30);
        Instant revokedAt = issuedAt.plusSeconds(60);
        User user = userRepository.save(User.builder()
                .firstName("Repository")
                .lastName("Contract")
                .email("refresh-repository-contract@example.test")
                .password("bcrypt-placeholder")
                .role(RoleEnum.USER)
                .build());
        RefreshTokenFamily family = new RefreshTokenFamily();
        family.setFamilyUuid(UUID.randomUUID());
        family.setUser(user);
        family.setIssuedAt(issuedAt);
        family.setAbsoluteExpiresAt(issuedAt.plusSeconds(3600));
        familyRepository.save(family);
        RefreshToken token = new RefreshToken();
        token.setFamily(family);
        token.setTokenHmac(new byte[32]);
        token.setHmacKeyId("active");
        token.setIssuedAt(issuedAt);
        token.setExpiresAt(family.getAbsoluteExpiresAt());
        tokenRepository.saveAndFlush(token);
        entityManager.clear();

        RefreshToken managedToken = tokenRepository.findById(token.getId()).orElseThrow();
        entityManager.setFlushMode(FlushModeType.COMMIT);
        managedToken.setLastPresentedAt(lastPresentedAt);

        tokenRepository.revokeAllForFamily(family.getId(), revokedAt);

        assertThat(entityManager.contains(managedToken)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_presented_at FROM refresh_tokens WHERE id = ?",
                java.sql.Timestamp.class,
                token.getId()).toLocalDateTime())
                .isEqualTo(lastPresentedAt.atOffset(ZoneOffset.UTC).toLocalDateTime());
        RefreshToken reloadedToken = tokenRepository.findById(token.getId()).orElseThrow();
        assertThat(reloadedToken.getRevokedAt()).isEqualTo(revokedAt);
    }

    @Test
    void validatesMappedEntitiesAndEnvironmentBackedSessionConfigurationWithoutPersistingSecrets() {
        assertThat(RefreshTokenFamily.class.getAnnotation(jakarta.persistence.Entity.class)).isNotNull();
        assertThat(RefreshToken.class.getAnnotation(jakarta.persistence.Entity.class)).isNotNull();
        assertThat(SessionSecurityEvent.class.getAnnotation(jakarta.persistence.Entity.class)).isNotNull();
        assertThat(sessionProperties.accessTokenLifetime()).hasSeconds(900);
        assertThat(sessionProperties.refresh().absoluteLifetime()).hasDays(30);
        assertThat(sessionProperties.keyRing().keys()).containsKey(sessionProperties.keyRing().activeKeyId());
        assertThat(sessionProperties.keyRing().keys().values()).allMatch(value -> !value.isBlank());

        List<String> secretBearingColumns = jdbcTemplate.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name IN ('refresh_token_families', 'refresh_tokens', 'session_security_events')
                  AND (column_name LIKE '%token%' OR column_name LIKE '%secret%' OR column_name LIKE '%hmac%')
                """, String.class);
        assertThat(secretBearingColumns).containsExactlyInAnyOrder("token_hmac", "hmac_key_id", "predecessor_token_id");
    }

    @Test
    void bindsProductionStyleIndexedKeyRingAndResolvesTheActiveKey() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("environment-style-key-ring", Map.ofEntries(
                Map.entry("app.session.access-token-lifetime", "PT15M"),
                Map.entry("app.session.refresh.enabled", "false"),
                Map.entry("app.session.refresh.absolute-lifetime", "P30D"),
                Map.entry("app.session.key-ring.active-key-id", "next"),
                Map.entry("app.session.key-ring.key-entries[0].id", "current"),
                Map.entry("app.session.key-ring.key-entries[0].secret", "current-environment-secret"),
                Map.entry("app.session.key-ring.key-entries[1].id", "next"),
                Map.entry("app.session.key-ring.key-entries[1].secret", "next-environment-secret"),
                Map.entry("app.session.cleanup.interval", "P1D"),
                Map.entry("app.session.cleanup.batch-size", "100"),
                Map.entry("app.session.rate-limit.refresh-per-family-per-minute", "10"),
                Map.entry("app.session.rate-limit.refresh-per-ip-per-minute", "60")
        )));

        SessionProperties properties = Binder.get(environment)
                .bind("app.session", Bindable.of(SessionProperties.class))
                .orElseThrow(() -> new IllegalStateException("Session properties did not bind"));

        assertThat(properties.keyRing().keys())
                .containsEntry("current", "current-environment-secret")
                .containsEntry("next", "next-environment-secret");
        assertThat(properties.keyRing().keys().get(properties.keyRing().activeKeyId()))
                .isEqualTo("next-environment-secret");
    }

    @Test
    void mapsRevocationReasonToTheFlywayVarchar32Contract() throws NoSuchFieldException {
        jakarta.persistence.Column column = RefreshTokenFamily.class
                .getDeclaredField("revocationReason")
                .getAnnotation(jakarta.persistence.Column.class);

        assertThat(column).isNotNull();
        assertThat(column.length()).isEqualTo(32);
        assertThat(columnType("refresh_token_families", "revocation_reason")).isEqualTo("varchar(32)");
    }

    private List<String> tableNames() {
        return jdbcTemplate.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = DATABASE()
                """, String.class);
    }

    private String columnType(String table, String column) {
        return jdbcTemplate.queryForObject("""
                SELECT column_type FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, String.class, table, column);
    }

    private List<String> indexNames(String table) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT index_name FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private Integer foreignKeyCount(String table) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.referential_constraints
                WHERE constraint_schema = DATABASE() AND table_name = ?
                """, Integer.class, table);
    }
}
