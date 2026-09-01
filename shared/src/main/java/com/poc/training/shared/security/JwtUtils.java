package com.poc.training.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

public final class JwtUtils {

    private JwtUtils() {}

    public static Jwt getJwtFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            return (Jwt) auth.getPrincipal();
        }
        return null;
    }

    public static boolean isEmployeeOnly(Jwt jwt) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !realmAccess.containsKey("roles")) return false;
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles.contains("employee") && !roles.contains("admin") && !roles.contains("instructor");
    }

    public static Long extractEmployeeIdAsLong(Jwt jwt) {
        String employeeId = extractEmployeeIdRaw(jwt);
        if (employeeId == null) return null;
        try {
            return Long.parseLong(employeeId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String extractEmployeeIdAsString(Jwt jwt) {
        return extractEmployeeIdRaw(jwt);
    }

    private static String extractEmployeeIdRaw(Jwt jwt) {
        if (jwt == null) return null;
        List<String> employeeIds = jwt.getClaimAsStringList("employeeId");
        if (employeeIds != null && !employeeIds.isEmpty()) {
            return employeeIds.get(0);
        }
        String scalar = jwt.getClaimAsString("employeeId");
        if (scalar != null && !scalar.isBlank() && !scalar.equals("[]")) {
            return scalar;
        }
        return null;
    }
}
