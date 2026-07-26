package com.poc.training.testsupport.container;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public final class PostgresTestContainer {

    private static final String IMAGE = "postgres:16-alpine";

    private PostgresTestContainer() {}

    private static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>(DockerImageName.parse(IMAGE)
                    .asCompatibleSubstituteFor("postgres"))
                    .withUsername("test")
                    .withPassword("test")
                    .withDatabaseName("test_db")
                    .withStartupTimeout(Duration.ofSeconds(300));

    public static PostgreSQLContainer<?> getInstance() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
            createDatabases();
        }
        return INSTANCE;
    }

    private static void createDatabases() {
        String url = INSTANCE.getJdbcUrl().replace("test_db", "postgres");
        try (var conn = java.sql.DriverManager.getConnection(url, INSTANCE.getUsername(), INSTANCE.getPassword());
             var stmt = conn.createStatement()) {
            for (String db : new String[]{"training_catalog", "training_enrollment",
                    "training_certification", "training_notification", "training_employee"}) {
                try {
                    stmt.executeUpdate("CREATE DATABASE " + db);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize test databases", e);
        }
    }
}
