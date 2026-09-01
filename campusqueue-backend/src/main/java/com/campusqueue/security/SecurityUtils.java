package com.campusqueue.security;

import com.campusqueue.entity.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Helper utility to access currently authenticated security principal
 * and enforce role/ownership rules in service layers.
 */
@Component
public class SecurityUtils {

    public static Optional<CustomUserDetails> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return Optional.of(customUserDetails);
        }
        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(CustomUserDetails::getId);
    }

    public static boolean isStaffOrAdmin() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.STAFF || u.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    public static boolean isAdmin() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.ADMIN)
                .orElse(false);
    }

    public static boolean isStudent() {
        return getCurrentUser()
                .map(u -> u.getRole() == UserRole.STUDENT)
                .orElse(false);
    }

    /**
     * Enforces that a STUDENT can only act on their own resources.
     * STAFF and ADMIN are allowed across any user.
     */
    public static void enforceUserOwnership(Long targetUserId, String actionDescription) {
        getCurrentUser().ifPresent(currentUser -> {
            if (currentUser.getRole() == UserRole.STUDENT) {
                if (targetUserId != null && !targetUserId.equals(currentUser.getId())) {
                    throw new AccessDeniedException("Access denied: You can only " + actionDescription + " for your own account.");
                }
            }
        });
    }
}
