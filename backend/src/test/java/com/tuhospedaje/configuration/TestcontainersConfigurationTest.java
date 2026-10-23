package com.tuhospedaje.configuration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestcontainersConfigurationTest {

    private static final String FAIL_CLOSED_DATABASE_URL =
            "jdbc:invalid:tuhospedaje-testcontainers-required";

    @Test
    void surefireProvidesFailClosedDatasourceUrlWithoutFlywayOverride() {
        assertThat(System.getProperty("spring.datasource.url")).isEqualTo(FAIL_CLOSED_DATABASE_URL);
        assertThat(System.getProperty("spring.flyway.url")).isNull();
    }

    @Test
    void rejectsReusableContainerBeforeStartingIt() {
        MariaDBContainer<?> container = mock(MariaDBContainer.class);
        when(container.isShouldBeReused()).thenReturn(true);

        assertThatThrownBy(() -> TestcontainersConfiguration.requireDisposable(container))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The test database container must be disposable");

        verify(container, never()).start();
    }

    @Test
    void rejectsFixedHostPortBeforeStartingContainer() {
        MariaDBContainer<?> container = mock(MariaDBContainer.class);
        when(container.getPortBindings()).thenReturn(List.of("3306:3306"));

        assertThatThrownBy(() -> TestcontainersConfiguration.requireDisposable(container))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The test database container must use a random host port");

        verify(container, never()).start();
    }

    @Test
    void factoryKeepsTheSharedContainerDisposableAndOnRandomHostPorts() {
        MariaDBContainer<?> container = new TestcontainersConfiguration().mariadbContainer();

        assertThat(container.isShouldBeReused()).isFalse();
        assertThat(container.getPortBindings()).isEmpty();
    }
}
