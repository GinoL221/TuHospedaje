package com.tuhospedaje.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MariaDBContainer<?> mariadbContainer() {
        MariaDBContainer<?> container = new MariaDBContainer<>(
                DockerImageName.parse("mariadb:10.11")
        ).withReuse(false);
        return requireDisposable(container);
    }

    static MariaDBContainer<?> requireDisposable(MariaDBContainer<?> container) {
        if (container.isShouldBeReused()) {
            throw new IllegalStateException("The test database container must be disposable");
        }
        if (!container.getPortBindings().isEmpty()) {
            throw new IllegalStateException("The test database container must use a random host port");
        }
        return container;
    }
}
