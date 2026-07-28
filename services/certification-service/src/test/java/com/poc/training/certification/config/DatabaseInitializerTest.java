package com.poc.training.certification.config;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;

class DatabaseInitializerTest {

    @Test
    void shouldCreateSequenceOnInit() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DatabaseInitializer initializer = new DatabaseInitializer(jdbcTemplate);

        initializer.init();

        verify(jdbcTemplate).execute(contains("certificate_number_seq"));
    }

    @Test
    void shouldHandleExceptionOnInit() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new RuntimeException("DB error")).when(jdbcTemplate).execute(anyString());
        DatabaseInitializer initializer = new DatabaseInitializer(jdbcTemplate);

        initializer.init();

        verify(jdbcTemplate).execute(anyString());
    }
}
