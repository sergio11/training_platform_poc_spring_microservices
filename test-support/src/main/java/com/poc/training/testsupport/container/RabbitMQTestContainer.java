package com.poc.training.testsupport.container;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public final class RabbitMQTestContainer {

    private static final String IMAGE = "rabbitmq:3.13-management-alpine";

    private RabbitMQTestContainer() {}

    private static final RabbitMQContainer INSTANCE =
            new RabbitMQContainer(DockerImageName.parse(IMAGE))
                    .withStartupTimeout(Duration.ofSeconds(120));

    public static RabbitMQContainer getInstance() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }
}
