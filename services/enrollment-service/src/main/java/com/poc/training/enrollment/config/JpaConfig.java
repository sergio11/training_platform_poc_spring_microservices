package com.poc.training.enrollment.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"com.poc.training.enrollment", "com.poc.training.shared"})
@EnableJpaRepositories(basePackages = {"com.poc.training.enrollment", "com.poc.training.shared"})
public class JpaConfig {
}
