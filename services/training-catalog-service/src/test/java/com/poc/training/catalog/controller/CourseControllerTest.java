package com.poc.training.catalog.controller;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void shouldReturnAllCourses() throws Exception {
        Course course = Course.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        when(courseService.findAll()).thenReturn(Arrays.asList(course));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java Basics"));
    }

    @Test
    void shouldReturnCourseById() throws Exception {
        Course course = Course.builder()
                .id(1L)
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .enrolledCount(0)
                .status(CourseStatus.ACTIVE)
                .build();

        when(courseService.findById(1L)).thenReturn(course);

        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    void shouldCreateCourse() throws Exception {
        Course course = Course.builder()
                .title("Java Basics")
                .instructor("John")
                .capacity(30)
                .build();

        when(courseService.create(any(Course.class))).thenReturn(course);

        mockMvc.perform(post("/api/v1/courses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Java Basics\",\"instructor\":\"John\",\"capacity\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    void shouldUpdateCourse() throws Exception {
        Course course = Course.builder()
                .id(1L)
                .title("Java Advanced")
                .instructor("John Updated")
                .capacity(40)
                .build();

        when(courseService.update(any(Long.class), any(Course.class))).thenReturn(course);

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
