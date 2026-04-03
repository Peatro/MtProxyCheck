package com.peatroxd.mtprototest.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    private final AdminAccessProperties adminAccessProperties;

    public AdminAccessInterceptor(AdminAccessProperties adminAccessProperties) {
        this.adminAccessProperties = adminAccessProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!adminAccessProperties.isProtectionActive()) {
            return true;
        }

        String path = request.getRequestURI();
        if (!requiresAdminAccess(path)) {
            return true;
        }

        String headerValue = request.getHeader(adminAccessProperties.getHeaderName());
        if (headerValue == null || !headerValue.equals(adminAccessProperties.getKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        return true;
    }

    private boolean requiresAdminAccess(String path) {
        return path.startsWith("/api/import")
                || path.startsWith("/api/check")
                || path.startsWith("/api/admin")
                || path.startsWith("/api/v1/import")
                || path.startsWith("/api/v1/check")
                || path.startsWith("/api/v1/admin");
    }
}
