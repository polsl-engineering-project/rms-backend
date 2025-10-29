package com.polsl.engineering.project.rms;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
public abstract class ContainersEnvironment {

    /*
     * Set reuse to false so that the container automatically shuts down
     * after the tests, when the JVM terminates.
     *
     * In this mode, we can’t use beforeAll or afterAll, because the container
     * stops after one Repository’s tests and is no longer available for the next one.
     */
    static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("rms_test_db")
                    .withUsername("rms_test_user")
                    .withPassword("rms_test_password")
                    .withReuse(false);



    @DynamicPropertySource
    static void registerDynamicProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        if (!postgreSQLContainer.isRunning()) {
            postgreSQLContainer.start();
        }
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

}
