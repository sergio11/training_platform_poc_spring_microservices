package com.poc.training.catalog;

import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class TrainingCatalogServiceApplicationTest extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
