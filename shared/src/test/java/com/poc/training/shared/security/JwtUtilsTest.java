package com.poc.training.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void shouldReturnNullWhenNoAuthentication() {
        SecurityContextHolder.clearContext();
        Jwt result = JwtUtils.getJwtFromContext();
        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenPrincipalIsNotJwt() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "not-a-jwt", "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_employee")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Jwt result = JwtUtils.getJwtFromContext();
        assertNull(result);

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnJwtWhenPrincipalIsJwt() {
        Jwt jwt = createJwt(Map.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(jwt, "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_employee")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Jwt result = JwtUtils.getJwtFromContext();
        assertNotNull(result);
        assertSame(jwt, result);

        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldDetectEmployeeOnlyRole() {
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("employee"))
        ));

        assertTrue(JwtUtils.isEmployeeOnly(jwt));
    }

    @Test
    void shouldNotBeEmployeeOnlyWhenAdminIncluded() {
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("employee", "admin"))
        ));

        assertFalse(JwtUtils.isEmployeeOnly(jwt));
    }

    @Test
    void shouldNotBeEmployeeOnlyWhenInstructorIncluded() {
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("employee", "instructor"))
        ));

        assertFalse(JwtUtils.isEmployeeOnly(jwt));
    }

    @Test
    void shouldReturnFalseWhenNoRealmAccess() {
        Jwt jwt = createJwt(Map.of());

        assertFalse(JwtUtils.isEmployeeOnly(jwt));
    }

    @Test
    void shouldReturnFalseWhenRealmAccessHasNoRoles() {
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("other", "value")
        ));

        assertFalse(JwtUtils.isEmployeeOnly(jwt));
    }

    @Test
    void shouldReturnFalseForNullJwt() {
        assertFalse(JwtUtils.isEmployeeOnly(null));
    }

    @Test
    void shouldReturnFalseWhenNoEmployeeRole() {
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("admin", "instructor"))
        ));

        assertFalse(JwtUtils.isEmployeeOnly(jwt));
    }

    @Test
    void shouldExtractEmployeeIdAsString() {
        Jwt jwt = createJwt(Map.of("employeeId", "42"));

        assertEquals("42", JwtUtils.extractEmployeeIdAsString(jwt));
    }

    @Test
    void shouldExtractEmployeeIdAsLong() {
        Jwt jwt = createJwt(Map.of("employeeId", "42"));

        assertEquals(42L, JwtUtils.extractEmployeeIdAsLong(jwt));
    }

    @Test
    void shouldReturnNullForNonNumericEmployeeId() {
        Jwt jwt = createJwt(Map.of("employeeId", "not-a-number"));

        assertNull(JwtUtils.extractEmployeeIdAsLong(jwt));
    }

    @Test
    void shouldExtractEmployeeIdFromList() {
        Jwt jwt = createJwt(Map.of("employeeId", List.of("99")));

        assertEquals(99L, JwtUtils.extractEmployeeIdAsLong(jwt));
    }

    @Test
    void shouldReturnNullForNullJwtEmployeeId() {
        assertNull(JwtUtils.extractEmployeeIdAsLong(null));
    }

    @Test
    void shouldReturnNullForNullJwtEmployeeIdString() {
        assertNull(JwtUtils.extractEmployeeIdAsString(null));
    }

    @Test
    void shouldReturnNullWhenEmployeeIdListIsEmpty() {
        Jwt jwt = createJwt(Map.of("employeeId", List.of()));

        String result = JwtUtils.extractEmployeeIdAsString(jwt);
        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenEmployeeIdClaimIsMissing() {
        Jwt jwt = createJwt(Map.of());

        assertNull(JwtUtils.extractEmployeeIdAsLong(jwt));
        assertNull(JwtUtils.extractEmployeeIdAsString(jwt));
    }

    @Test
    void shouldReturnTrueWhenOnlyEmployeeRoleWithoutAdminOrInstructor() {
        Jwt jwt = createJwt(Map.of(
                "realm_access", Map.of("roles", List.of("employee", "user"))
        ));

        assertTrue(JwtUtils.isEmployeeOnly(jwt));
    }
}
