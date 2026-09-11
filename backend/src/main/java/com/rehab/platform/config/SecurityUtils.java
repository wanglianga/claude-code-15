package com.rehab.platform.config;

import com.rehab.platform.enums.Role;
import com.rehab.platform.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        throw new BusinessException(401, "未登录或登录已过期");
    }

    public static void requireRole(Role... roles) {
        User user = currentUser();
        boolean ok = Arrays.stream(roles).anyMatch(r -> r == user.getRole());
        if (!ok) {
            throw new BusinessException(403, "当前角色（" + user.getRole().getLabel() + "）无权执行此操作");
        }
    }
}
