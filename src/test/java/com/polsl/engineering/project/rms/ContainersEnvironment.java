package com.polsl.engineering.project.rms;

import org.flywaydb.core.Flyway;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SuppressWarnings("resource")
public abstract class ContainersEnvironment {

    private static final Logger log = LoggerFactory.getLogger(ContainersEnvironment.class);

    /*
     * Set reuse to false so that the container automatically shuts down
     * after the tests, when the JVM terminates.
     *
     * In this mode, we can’t use beforeAll or afterAll, because the container
     * stops after one Repository’s tests and is no longer available for the next one.
     */
    protected static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("rms_test_db")
                    .withUsername("rms_test_user")
                    .withPassword("rms_test_password")
                    .withReuse(false);



    @DynamicPropertySource
    static void registerDynamicProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        var start = false;
        if (!postgreSQLContainer.isRunning()) {
            postgreSQLContainer.start();
            start = true;
        }
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        // Disable Spring Boot's automatic Flyway execution because we run Flyway manually once on container startup.
        registry.add("spring.flyway.enabled", () -> "false");

        // Run Flyway baseline/clean + migrate once when container is started to ensure a clean schema for tests.
        // This avoids race conditions and "relation already exists" errors when multiple test classes run.
        if (start) {
            try {
                log.info("Running Flyway clean+migrate for test container");
                Flyway.configure()
                        .dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword())
                        .locations("classpath:db/migration")
                        .cleanDisabled(false)
                        .baselineOnMigrate(true)
                        .load()
                        .clean();
                var flyway = Flyway.configure()
                        .dataSource(postgreSQLContainer.getJdbcUrl(), postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword())
                        .locations("classpath:db/migration")
                        .cleanDisabled(false)
                        .baselineOnMigrate(true)
                        .load();
                flyway.migrate();
            } catch (Exception e) {
                log.warn("Flyway clean/migrate during container startup failed", e);
                throw e;
            }
        }
    }

}
