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
}
