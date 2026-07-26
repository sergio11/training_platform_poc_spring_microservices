package com.poc.training.testsupport;

import com.poc.training.testsupport.container.PostgresTestContainer;
import com.poc.training.testsupport.container.RabbitMQTestContainer;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    static {
        System.setProperty("testcontainers.ryuk.disabled", "true");
        System.setProperty("ryuk.disabled", "true");
        if (System.getenv("DOCKER_HOST") == null) {
            System.setProperty("docker.host", "npipe:////./pipe/podman-machine-default");
        }
    }

    protected static final PostgreSQLContainer<?> postgres = PostgresTestContainer.getInstance();
    protected static final RabbitMQContainer rabbitmq = RabbitMQTestContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String dbName = resolveDatabaseName();
        String jdbcUrl = postgres.getJdbcUrl().replace("test_db", dbName);
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    private static String resolveDatabaseName() {
        String userDir = System.getProperty("user.dir").toLowerCase();
        if (userDir.contains("certification")) return "training_certification";
        if (userDir.contains("employee")) return "training_employee";
        if (userDir.contains("enrollment")) return "training_enrollment";
        if (userDir.contains("notification")) return "training_notification";
        if (userDir.contains("catalog")) return "training_catalog";

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName().toLowerCase();
            if (className.contains("certification")) return "training_certification";
            if (className.contains("employee")) return "training_employee";
            if (className.contains("enrollment")) return "training_enrollment";
            if (className.contains("notification")) return "training_notification";
            if (className.contains("catalog")) return "training_catalog";
        }
        return "test_db";
    }
}
