package com.poc.training.shared.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceUnavailableExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        ServiceUnavailableException ex = new ServiceUnavailableException("Service down");
        assertThat(ex.getMessage()).isEqualTo("Service down");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("connection refused");
        ServiceUnavailableException ex = new ServiceUnavailableException("Service down", cause);
        assertThat(ex.getMessage()).isEqualTo("Service down");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
