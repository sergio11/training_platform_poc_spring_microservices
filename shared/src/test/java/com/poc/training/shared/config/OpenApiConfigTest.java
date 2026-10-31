package com.poc.training.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void shouldBuildOpenApiMetadataWithBearerAuth() {
        OpenAPI openApi = new OpenApiConfig().trainingPlatformOpenApi("training-catalog-service");

        assertEquals("training-catalog-service API", openApi.getInfo().getTitle());
        assertEquals("Training Platform POC - REST API", openApi.getInfo().getDescription());
        assertEquals("1.0.0", openApi.getInfo().getVersion());

        SecurityScheme scheme = openApi.getComponents().getSecuritySchemes()
                .get(OpenApiConfig.BEARER_AUTH);
        assertNotNull(scheme);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());

        assertTrue(openApi.getSecurity().stream()
                .anyMatch(requirement -> requirement.containsKey(OpenApiConfig.BEARER_AUTH)));
    }

    @Test
    void shouldUseDefaultApplicationName() {
        OpenAPI openApi = new OpenApiConfig().trainingPlatformOpenApi("training-platform");

        assertEquals("training-platform API", openApi.getInfo().getTitle());
    }
}
