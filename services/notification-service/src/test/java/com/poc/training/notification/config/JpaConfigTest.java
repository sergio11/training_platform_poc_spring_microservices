package com.poc.training.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

import static org.assertj.core.api.Assertions.assertThat;

class JpaConfigTest {

    @Test
    void shouldHaveConfigurationAnnotation() {
        assertThat(JpaConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
    }

    @Test
    void shouldHaveEntityScanWithCorrectPackages() {
        EntityScan entityScan = JpaConfig.class.getAnnotation(EntityScan.class);
        assertThat(entityScan).isNotNull();
        assertThat(entityScan.basePackages()).containsExactlyInAnyOrder("com.poc.training.notification", "com.poc.training.shared");
    }

    @Test
    void shouldHaveEnableJpaRepositoriesWithCorrectPackages() {
        EnableJpaRepositories enableJpa = JpaConfig.class.getAnnotation(EnableJpaRepositories.class);
        assertThat(enableJpa).isNotNull();
        assertThat(enableJpa.basePackages()).containsExactlyInAnyOrder("com.poc.training.notification", "com.poc.training.shared");
    }

    @Test
    void shouldBeInstantiable() {
        JpaConfig config = new JpaConfig();
        assertThat(config).isNotNull();
    }
}
