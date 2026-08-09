package com.poc.training.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakJwtConverterTest {

    @Test
    void shouldCreateConverter() {
        var converter = KeycloakJwtConverter.create();
        assertNotNull(converter);
    }

    @Test
    void shouldExtractRolesFromRealmAccess() {
        var converter = KeycloakJwtConverter.create();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .claim("realm_access", Map.of("roles", List.of("employee", "admin")))
                .build();

        var auth = converter.convert(jwt);
        assertNotNull(auth);
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_employee")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_admin")));
    }

    @Test
    void shouldReturnEmptyWhenNoRealmAccess() {
        var converter = KeycloakJwtConverter.create();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .build();

        var auth = converter.convert(jwt);
        assertNotNull(auth);
        assertFalse(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_employee")));
        assertFalse(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_admin")));
    }

    @Test
    void shouldReturnEmptyWhenRolesEmpty() {
        var converter = KeycloakJwtConverter.create();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .claim("realm_access", Map.of("other", "value"))
                .build();

        var auth = converter.convert(jwt);
        assertNotNull(auth);
        assertFalse(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_employee")));
        assertFalse(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_admin")));
    }

    @Test
    void shouldPrefixRolesWithRole() {
        var converter = KeycloakJwtConverter.create();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("subject")
                .claim("realm_access", Map.of("roles", List.of("instructor")))
                .build();

        var auth = converter.convert(jwt);
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_instructor")));
    }
}
