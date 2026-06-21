package com.poc.training.catalog.controller;

import com.poc.training.catalog.dto.CourseRequest;
import com.poc.training.catalog.dto.CourseResponse;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
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
        CourseResponse courseResponse = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE.name())
                .build();

        when(courseService.findAll(any())).thenReturn(new PageImpl<>(Arrays.asList(courseResponse)));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java Basics"));
    }

    @Test
    void shouldReturnCourseById() throws Exception {
        CourseResponse courseResponse = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE.name())
                .build();

        when(courseService.findById(1L)).thenReturn(courseResponse);

        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    void shouldCreateCourse() throws Exception {
        CourseResponse courseResponse = CourseResponse.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .build();

        when(courseService.create(any(CourseRequest.class))).thenReturn(courseResponse);

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java Basics\",\"instructor\":\"John\",\"capacity\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    void shouldUpdateCourse() throws Exception {
        CourseResponse courseResponse = CourseResponse.builder()
                .id(1L)
                .title("Java Advanced")
                .instructor("John Updated")
                .capacity(40)
                .build();

        when(courseService.update(any(Long.class), any(CourseRequest.class))).thenReturn(courseResponse);

        mockMvc.perform(put("/api/v1/courses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java Advanced\",\"instructor\":\"John Updated\",\"capacity\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Advanced"));
    }

    @Test
    void shouldDeleteCourse() throws Exception {
        mockMvc.perform(delete("/api/v1/courses/1"))
                .andExpect(status().isNoContent());
    }
}
