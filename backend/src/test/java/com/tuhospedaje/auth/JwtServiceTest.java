package com.tuhospedaje.auth;

import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("Juan")
                .email("juanperez@test.com")
                .password("hasheadpass")
                .role(RoleEnum.USER)
                .imageUrl("https://ui-avatars.com/api/?name=Juan+Pérez")
                .build();
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("juanperez@test.com");
    }

    @Test
    void shouldExposeExpirationInSeconds() {
        // app.jwt.expiration is configured as 28800000 ms in test resources.
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(28800L);
    }

    @Test
    void shouldRejectTokenWhenTokenVersionDoesNotMatchUser() {
        // testUser has default tokenVersion = 1
        String tokenV1 = jwtService.generateToken(testUser);
        assertThat(jwtService.isTokenValid(tokenV1, testUser)).isTrue();

        // Simulate password change incrementing tokenVersion
        testUser.setTokenVersion(2);
        assertThat(jwtService.isTokenValid(tokenV1, testUser)).isFalse();

        // Fresh token issued at version 2 is valid
        String tokenV2 = jwtService.generateToken(testUser);
        assertThat(jwtService.isTokenValid(tokenV2, testUser)).isTrue();
    }
}
