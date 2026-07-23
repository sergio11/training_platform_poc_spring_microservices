package com.poc.training.certification;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.sql.Statement;

@SpringBootApplication(scanBasePackages = {"com.poc.training.certification", "com.poc.training.shared"})
@EnableScheduling
public class CertificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertificationServiceApplication.class, args);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    CommandLineRunner ensureSequenceExists(DataSource dataSource) {
        return args -> {
            try (var conn = dataSource.getConnection()) {
                Statement stmt = conn.createStatement();
                stmt.execute("CREATE SEQUENCE IF NOT EXISTS certificate_number_seq START WITH 1 INCREMENT BY 1");
                stmt.close();
            }
        };
    }
}
