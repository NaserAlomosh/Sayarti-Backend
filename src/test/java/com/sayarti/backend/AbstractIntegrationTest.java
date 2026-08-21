package com.sayarti.backend;

import com.sayarti.backend.email.TestEmailConfiguration;
import com.sayarti.backend.email.TestEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
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

    @BeforeEach
    void resetTestEmailService() {
        testEmailService.clear();
    }
}
