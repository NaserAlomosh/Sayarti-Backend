package com.sayarti.backend;

import com.sayarti.backend.email.TestEmailConfiguration;
import com.sayarti.backend.email.TestEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MSSQLServerContainer;

@Import(TestEmailConfiguration.class)
public abstract class AbstractIntegrationTest {
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL_SERVER =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense();

    static {
        // Keep the database alive for every test class that reuses Spring's cached context.
        SQL_SERVER.start();
    }

    @Autowired
    protected TestEmailService testEmailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTestState() {
        jdbcTemplate.update("DELETE FROM devices");
        jdbcTemplate.update("DELETE FROM reminders");
        jdbcTemplate.update("DELETE FROM expenses");
        jdbcTemplate.update("DELETE FROM maintenance_records");
        jdbcTemplate.update("DELETE FROM fuel_records");
        jdbcTemplate.update("DELETE FROM vehicles");
        jdbcTemplate.update("DELETE FROM email_verification_otps");

        // Rotation links refresh-token rows to other rows in the same table. SQL Server
        // checks that self-referencing FK during a bulk delete, so detach the test data first.
        jdbcTemplate.update("UPDATE refresh_tokens SET replaced_by_token_id = NULL");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");

        testEmailService.clear();
    }
}
