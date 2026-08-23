package com.poc.training.employee.config;

import com.poc.training.employee.controller.EmployeeController;
import com.poc.training.employee.service.EmployeeService;
import com.poc.training.shared.security.KeycloakJwtConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri="
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @SuppressWarnings("unchecked")
    private Converter<Jwt, Collection<GrantedAuthority>> getGrantedAuthoritiesConverter() throws Exception {
        JwtAuthenticationConverter converter = KeycloakJwtConverter.create();
        Field field = JwtAuthenticationConverter.class.getDeclaredField("jwtGrantedAuthoritiesConverter");
        field.setAccessible(true);
        return (Converter<Jwt, Collection<GrantedAuthority>>) field.get(converter);
    }

    private Jwt buildJwt(Map<String, Object> realmAccess) {
        var builder = Jwt.withTokenValue("test-token")
                .headers(h -> h.put("alg", "none"))
                .claim("sub", "user1");
        if (realmAccess != null) {
            builder.claim("realm_access", realmAccess);
        }
        return builder.build();
    }

    @Test
    void shouldReturnEmptyListWhenRealmAccessIsNull() throws Exception {
        Converter<Jwt, Collection<GrantedAuthority>> converter = getGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> authorities = converter.convert(buildJwt(null));
        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenRolesKeyMissing() throws Exception {
        Converter<Jwt, Collection<GrantedAuthority>> converter = getGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> authorities = converter.convert(buildJwt(Map.of("other", "value")));
        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldMapRolesToAuthorities() throws Exception {
        Converter<Jwt, Collection<GrantedAuthority>> converter = getGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> authorities = converter.convert(buildJwt(Map.of("roles", List.of("admin", "employee"))));
        assertThat(authorities).hasSize(2);
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_admin", "ROLE_employee");
    }

    @Test
    void shouldReturnEmptyListWhenRolesIsEmpty() throws Exception {
        Converter<Jwt, Collection<GrantedAuthority>> converter = getGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> authorities = converter.convert(buildJwt(Map.of("roles", List.of())));
        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/employees").param("sortBy", "id"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAdminAccessToAllEmployees() throws Exception {
        org.springframework.data.domain.Page<?> emptyPage =
                new org.springframework.data.domain.PageImpl<>(List.of());
        when(employeeService.findAll(any())).thenReturn((org.springframework.data.domain.Page) emptyPage);

        mockMvc.perform(get("/api/v1/employees")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyEmployeeAccessToAllEmployees() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminAccessById() throws Exception {
        when(employeeService.findById(1L)).thenReturn(
                com.poc.training.employee.dto.EmployeeResponse.builder()
                        .id(1L).employeeNumber("EMP001").fullName("Test")
                        .email("test@test.com").active(true).build());

        mockMvc.perform(get("/api/v1/employees/1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk());
    }
}
