package com.schoolsaas;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FlywayConfigurationTests extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void connectsToPostgresAndRunsMigrationsOnStartup() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(flyway.info().all()).isNotEmpty();
        assertThat(flyway.info().pending()).isEmpty();
    }
}
