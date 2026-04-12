package com.peatroxd.mtprototest.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientRequestKeyResolver {

    public String resolve(HttpServletRequest request) {
        String fingerprint = firstNonBlank(request.getHeader("X-Client-Fingerprint"));
        if (fingerprint != null) {
            return fingerprint;
        }

        String forwardedFor = firstForwardedFor(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        String realIp = firstNonBlank(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String firstForwardedFor(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        for (String part : headerValue.split(",")) {
            String candidate = firstNonBlank(part);
            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    private String firstNonBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
