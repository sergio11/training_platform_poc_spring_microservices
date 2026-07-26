package com.poc.training.catalog;

import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TrainingCatalogServiceApplicationTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
