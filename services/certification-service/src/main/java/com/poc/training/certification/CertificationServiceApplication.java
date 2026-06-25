package com.poc.training.certification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.poc.training")
@EnableScheduling
public class CertificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertificationServiceApplication.class, args);
    }
}
