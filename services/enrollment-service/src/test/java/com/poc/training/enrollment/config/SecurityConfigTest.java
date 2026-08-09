package com.poc.training.enrollment.config;

import com.poc.training.shared.security.KeycloakJwtConverter;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

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
}
