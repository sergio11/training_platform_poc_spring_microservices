package com.poc.training.testsupport.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

public final class KeycloakTestContainer {

    private static final String IMAGE = "quay.io/keycloak/keycloak:24.0.1";
    private static final int PORT = 8080;

    private KeycloakTestContainer() {}

    private static final GenericContainer<?> INSTANCE =
            new GenericContainer<>(DockerImageName.parse(IMAGE))
                    .withExposedPorts(PORT)
                    .withCommand("start-dev", "--import-realm")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("keycloak/realm-export.json"),
                            "/opt/keycloak/data/import/realm-export.json")
                    .withStartupTimeout(Duration.ofSeconds(240))
                    .waitingFor(Wait.forLogMessage(".*Listening on:.*", 1)
                            .withStartupTimeout(Duration.ofSeconds(240)));

    public static GenericContainer<?> getInstance() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }

    public static int getPort() {
        return getInstance().getMappedPort(PORT);
    }

    public static String getIssuerUri() {
        return "http://localhost:" + getPort() + "/realms/training-realm";
    }
}
