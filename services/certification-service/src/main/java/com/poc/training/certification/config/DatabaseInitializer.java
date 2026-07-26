package com.poc.training.certification.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS certificate_number_seq START WITH 1 INCREMENT BY 1");
            log.info("Ensured certificate_number_seq exists in database.");
        } catch (Exception e) {
            log.warn("Could not create certificate_number_seq: {}", e.getMessage());
        }
    }
}
