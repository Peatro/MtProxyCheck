package com.peatroxd.mtprototest.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class AdminAccessFilter extends OncePerRequestFilter {

    private final AdminAccessProperties adminAccessProperties;
    private final ObjectMapper objectMapper;

    public AdminAccessFilter(AdminAccessProperties adminAccessProperties, ObjectMapper objectMapper) {
        this.adminAccessProperties = adminAccessProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!adminAccessProperties.isProtectionActive() || !requiresAdminAccess(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerValue = request.getHeader(adminAccessProperties.getHeaderName());
        if (headerValue != null && headerValue.equals(adminAccessProperties.getKey())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.FORBIDDEN.value(),
                "error", HttpStatus.FORBIDDEN.getReasonPhrase(),
                "code", "FORBIDDEN",
                "message", "Admin access required",
                "path", request.getRequestURI(),
                "details", List.of()
        ));
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
