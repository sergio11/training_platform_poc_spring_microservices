package com.poc.training.enrollment.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class CourseDTOTest {

    @Test
    void shouldCreateCourseDTOWithBuilder() {
        CourseDTO dto = CourseDTO.builder()
                .id(1L)
                .title("Java Basics")
                .description("Intro to Java")
                .instructor("John")
                .capacity(30)
                .enrolledCount(5)
                .status("ACTIVE")
                .build();

        assertEquals(1L, dto.getId());
        assertEquals("Java Basics", dto.getTitle());
        assertEquals("Intro to Java", dto.getDescription());
        assertEquals("John", dto.getInstructor());
        assertEquals(30, dto.getCapacity());
        assertEquals(5, dto.getEnrolledCount());
        assertEquals("ACTIVE", dto.getStatus());
    }

    @Test
    void shouldHaveNoArgsConstructor() {
        CourseDTO dto = new CourseDTO();
        assertNotNull(dto);
    }

    @Test
    void shouldHaveAllArgsConstructor() {
        CourseDTO dto = new CourseDTO(1L, "Title", "Desc", "Inst", 30, 0, "ACTIVE");
        assertEquals("Title", dto.getTitle());
        assertEquals("Desc", dto.getDescription());
        assertEquals("Inst", dto.getInstructor());
    }

    @Test
    void shouldSetAndGetProperties() {
        CourseDTO dto = new CourseDTO();
        dto.setId(1L);
        dto.setTitle("Test");
        dto.setDescription("Desc");
        dto.setInstructor("Inst");
        dto.setCapacity(10);
        dto.setEnrolledCount(5);
        dto.setStatus("INACTIVE");

        assertEquals(1L, dto.getId());
        assertEquals("Test", dto.getTitle());
        assertEquals("Desc", dto.getDescription());
        assertEquals("Inst", dto.getInstructor());
        assertEquals(10, dto.getCapacity());
        assertEquals(5, dto.getEnrolledCount());
        assertEquals("INACTIVE", dto.getStatus());
    }
}
