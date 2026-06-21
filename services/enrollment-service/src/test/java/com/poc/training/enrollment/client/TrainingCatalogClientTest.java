package com.poc.training.enrollment.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingCatalogClientTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient.Builder chainedBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private TrainingCatalogClient trainingCatalogClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(chainedBuilder);
        when(chainedBuilder.build()).thenReturn(webClient);
        trainingCatalogClient = new TrainingCatalogClient(webClientBuilder, "http://localhost:8081");
    }

    @Test
    void shouldThrowExceptionInGetCourseFallback() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> trainingCatalogClient.getCourseFallback(1L, new RuntimeException("Connection refused")));
        assertTrue(exception.getMessage().contains("Catalog service unavailable"));
        assertTrue(exception.getMessage().contains("1"));
    }

    @Test
    void shouldThrowExceptionInIncrementEnrolledCountFallback() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> trainingCatalogClient.incrementEnrolledCountFallback(1L, new RuntimeException("Connection refused")));
        assertTrue(exception.getMessage().contains("Catalog service unavailable"));
        assertTrue(exception.getMessage().contains("1"));
    }

    @Test
    void shouldThrowExceptionInGetCourseFallbackWithNullId() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> trainingCatalogClient.getCourseFallback(null, new RuntimeException("Error")));
        assertTrue(exception.getMessage().contains("Catalog service unavailable"));
    }

    @Test
    void shouldCallWebClientWhenGetCourse() {
        CourseDTO course = CourseDTO.builder().id(1L).title("Java Basics").capacity(30).enrolledCount(0).status("ACTIVE").build();

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CourseDTO.class)).thenReturn(Mono.just(course));

        CourseDTO result = trainingCatalogClient.getCourse(1L);

        assertNotNull(result);
        assertEquals("Java Basics", result.getTitle());
        verify(webClient).get();
    }

    @Test
    void shouldHaveWebClientField() {
        assertNotNull(trainingCatalogClient);
    }
}
