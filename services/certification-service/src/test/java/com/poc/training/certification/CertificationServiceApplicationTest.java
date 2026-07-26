package com.poc.training.certification;

import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CertificationServiceApplicationTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
