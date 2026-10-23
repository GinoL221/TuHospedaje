package com.tuhospedaje.configuration;

import com.tuhospedaje.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.MariaDBContainer;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TestcontainersConfigurationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MariaDBContainer<?> mariadbContainer;

    @Test
    void applicationAndFlywayConnectToTheSharedContainerDatabase() throws SQLException {
        DataSource flywayDataSource = flyway.getConfiguration().getDataSource();
        assertThat(flywayDataSource).isNotNull();

        URI containerUri = jdbcUri(mariadbContainer.getJdbcUrl());
        DatabaseIdentity containerDatabase = new DatabaseIdentity(
                containerUri.getHost(),
                containerUri.getPort(),
                mariadbContainer.getDatabaseName()
        );

        assertThat(databaseIdentity(dataSource)).isEqualTo(containerDatabase);
        assertThat(databaseIdentity(flywayDataSource)).isEqualTo(containerDatabase);
    }

    private DatabaseIdentity databaseIdentity(DataSource source) throws SQLException {
        try (Connection connection = source.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            assertThat(resultSet.next()).isTrue();
            URI connectionUri = jdbcUri(connection.getMetaData().getURL());
            return new DatabaseIdentity(
                    connectionUri.getHost(),
                    connectionUri.getPort(),
                    resultSet.getString(1)
            );
        }
    }

    private URI jdbcUri(String jdbcUrl) {
        assertThat(jdbcUrl).startsWith("jdbc:mariadb://");
        return URI.create(jdbcUrl.substring("jdbc:".length()));
    }

    private record DatabaseIdentity(String host, int port, String databaseName) {
    }
}
