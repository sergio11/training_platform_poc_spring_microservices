package com.poc.training.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.poc.training.catalog", "com.poc.training.shared"})
public class TrainingCatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingCatalogServiceApplication.class, args);
    }
}
