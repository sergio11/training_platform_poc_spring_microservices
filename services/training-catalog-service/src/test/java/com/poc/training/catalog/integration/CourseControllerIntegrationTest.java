package com.poc.training.catalog.integration;

import com.poc.training.catalog.model.Course;
import com.poc.training.catalog.model.CourseStatus;
import com.poc.training.catalog.repository.CourseRepository;
import com.poc.training.testsupport.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class CourseControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
    }

    @Test
    void shouldGetAllCourses() throws Exception {
        courseRepository.save(Course.builder()
                .title("Course1").instructor("I1").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        mockMvc.perform(get("/api/v1/courses")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void shouldCreateCourse() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Course\",\"description\":\"Test\",\"instructor\":\"John\",\"capacity\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("New Course"));
    }

    @Test
    void shouldGetCourseById() throws Exception {
        Course saved = courseRepository.save(Course.builder()
                .title("FindById").instructor("I").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        mockMvc.perform(get("/api/v1/courses/{id}", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("FindById"));
    }

    @Test
    void shouldUpdateCourse() throws Exception {
        Course saved = courseRepository.save(Course.builder()
                .title("Old Title").instructor("I").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        mockMvc.perform(put("/api/v1/courses/{id}", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\",\"description\":\"Updated\",\"instructor\":\"New\",\"capacity\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"));
    }

    @Test
    void shouldDeactivateCourse() throws Exception {
        Course saved = courseRepository.save(Course.builder()
                .title("To Delete").instructor("I").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());

        mockMvc.perform(delete("/api/v1/courses/{id}", saved.getId())
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404ForNonExistentCourse() throws Exception {
        mockMvc.perform(get("/api/v1/courses/{id}", 99999)
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"capacity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFilterCoursesByStatus() throws Exception {
        courseRepository.save(Course.builder().title("Active1").instructor("I").capacity(10).enrolledCount(0).status(CourseStatus.ACTIVE).build());
        courseRepository.save(Course.builder().title("Inactive1").instructor("I").capacity(10).enrolledCount(0).status(CourseStatus.INACTIVE).build());

        mockMvc.perform(get("/api/v1/courses")
                        .param("status", "ACTIVE")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Active1"));
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForEmployeeTryingToCreate() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .with(jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fail\",\"instructor\":\"X\",\"capacity\":1}"))
                .andExpect(status().isForbidden());
    }
}
