package com.poc.training.testsupport.container;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class PostgresTestContainer {

    private static final String IMAGE = "postgres:16-alpine";

    private PostgresTestContainer() {}

    private static final PostgreSQLContainer<?> INSTANCE =
            new PostgreSQLContainer<>(DockerImageName.parse(IMAGE)
                    .asCompatibleSubstituteFor("postgres"))
                    .withUsername("test")
                    .withPassword("test")
                    .withDatabaseName("test_db");

    public static PostgreSQLContainer<?> getInstance() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }
}
