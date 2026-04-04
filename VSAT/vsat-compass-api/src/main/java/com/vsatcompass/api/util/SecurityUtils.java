package com.vsatcompass.api.util;

import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.security.service.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static CustomUserDetails getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) auth.getPrincipal();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        CustomUserDetails details = getCurrentUserDetails();
        return details != null ? details.getId() : null;
    }

    public static String getCurrentUserRole() {
        CustomUserDetails details = getCurrentUserDetails();
        return details != null ? details.getRole() : null;
    }

    public static boolean hasRole(UserRole role) {
        String currentRole = getCurrentUserRole();
        return currentRole != null && currentRole.equals(role.name());
    }

    public static boolean isAdmin() {
        String role = getCurrentUserRole();
        return role != null && (role.equals(UserRole.CONTENT_ADMIN.name()) || role.equals(UserRole.SUPER_ADMIN.name()));
    }

    public static boolean isSuperAdmin() {
        return hasRole(UserRole.SUPER_ADMIN);
    }
}
