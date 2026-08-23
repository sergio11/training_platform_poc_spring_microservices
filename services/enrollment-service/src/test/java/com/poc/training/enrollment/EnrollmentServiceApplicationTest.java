package com.poc.training.enrollment;

import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class EnrollmentServiceApplicationTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
