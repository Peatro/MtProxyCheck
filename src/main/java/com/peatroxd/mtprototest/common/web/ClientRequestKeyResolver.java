package com.peatroxd.mtprototest.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientRequestKeyResolver {

    public String resolve(HttpServletRequest request) {
        String fingerprint = request.getHeader("X-Client-Fingerprint");
        if (fingerprint != null && !fingerprint.isBlank()) {
            return fingerprint.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
