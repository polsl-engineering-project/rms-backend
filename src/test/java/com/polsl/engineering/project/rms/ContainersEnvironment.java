package com.polsl.engineering.project.rms;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
public abstract class ContainersEnvironment {

    static PostgreSQLContainer<?> postgreSQLContainer =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("rms_test_db")
                    .withUsername("rms_test_user")
                    .withPassword("rms_test_password")
                    .withReuse(false);
                    // reuse na false, wtedy kontener sam kończy swój żywot autamatycznie po testach czyli po zamknięciu JVM
                    // wtedy też nie możemy używać beforeAll i AfterAll bo kontener zamykał się po testach jednego Repository i dla drugiego Repository był już niedostępny


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
