package com.poc.training.catalog.controller;

import com.poc.training.catalog.dto.CourseRequest;
import com.poc.training.catalog.dto.CourseResponse;
import com.poc.training.catalog.service.CourseService;
import com.poc.training.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.poc.training.shared.exception.GlobalExceptionHandler;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void shouldReturnAllCourses() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status("ACTIVE")
                .build();

        when(courseService.search(any(), any(), any(), any())).thenReturn(new PageImpl<>(Arrays.asList(response)));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Basics"));
    }

    @Test
    void shouldReturnPaginatedCourses() throws Exception {
        CourseResponse r1 = CourseResponse.builder()
                .id(1L).title("Java Basics").instructor("John").capacity(30).enrolledCount(0).status("ACTIVE").build();
        CourseResponse r2 = CourseResponse.builder()
                .id(2L).title("Spring Boot").instructor("Jane").capacity(25).enrolledCount(0).status("ACTIVE").build();

        org.springframework.data.domain.Page<CourseResponse> page =
                new PageImpl<>(Arrays.asList(r1, r2),
                        org.springframework.data.domain.PageRequest.of(0, 20), 2);

        when(courseService.search(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/courses")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void shouldReturnEmptyPageWhenNoCourses() throws Exception {
        org.springframework.data.domain.Page<CourseResponse> emptyPage =
                new PageImpl<>(Arrays.asList(),
                        org.springframework.data.domain.PageRequest.of(0, 20), 0);

        when(courseService.search(any(), any(), any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void shouldReturnCourseById() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status("ACTIVE")
                .build();

        when(courseService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    void shouldCreateCourse() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status("ACTIVE")
                .build();

        when(courseService.create(any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java Basics\",\"instructor\":\"John\",\"capacity\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    void shouldUpdateCourse() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Java Advanced")
                .instructor("John Updated")
                .capacity(40)
                .enrolledCount(0)
                .status("ACTIVE")
                .build();

        when(courseService.update(eq(1L), any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java Advanced\",\"instructor\":\"John Updated\",\"capacity\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Advanced"));
    }

    @Test
    void shouldReturn400WhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"instructor\":\"John\",\"capacity\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Title is required")));
    }

    @Test
    void shouldReturn400WhenInstructorIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java\",\"instructor\":\"\",\"capacity\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Instructor is required")));
    }

    @Test
    void shouldReturn400WhenCapacityIsZero() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java\",\"instructor\":\"John\",\"capacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Capacity must be at least 1")));
    }

    @Test
    void shouldReturn400WhenCapacityIsNull() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Java\",\"instructor\":\"John\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Capacity is required")));
    }

    @Test
    void shouldReturn400WhenBodyIsInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteCourse() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnActiveCourses() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status("ACTIVE")
                .build();

        when(courseService.findActiveCourses(any())).thenReturn(new PageImpl<>(Arrays.asList(response)));

        mockMvc.perform(get("/api/v1/courses/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Basics"));
    }

    @Test
    void shouldMarkCourseAsCompleted() throws Exception {
        CourseResponse response = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status("COMPLETED")
                .build();

        when(courseService.markAsCompleted(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/courses/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldReturn400WhenMarkingNonActiveCourseAsCompleted() throws Exception {
        doThrow(new BusinessException("Cannot complete course in status: INACTIVE. Expected: ACTIVE"))
                .when(courseService).markAsCompleted(1L);
        mockMvc.perform(patch("/api/v1/courses/1/complete"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenSortByIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/courses")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldIncrementEnrolledCount() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/1/seats/increment"))
                .andExpect(status().isOk());
        verify(courseService).incrementEnrolledCount(1L);
    }

    @Test
    void shouldDecrementEnrolledCount() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/1/seats/decrement"))
                .andExpect(status().isOk());
        verify(courseService).decrementEnrolledCount(1L);
    }
}
