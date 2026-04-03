package com.peatroxd.mtprototest.parser.service;

import com.peatroxd.mtprototest.parser.model.RawProxy;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class RawProxyNormalizer {

    public Optional<RawProxy> normalize(RawProxy rawProxy) {
        if (rawProxy == null) {
            return Optional.empty();
        }

        String host = normalizeHost(rawProxy.host());
        Integer port = rawProxy.port();
        String secret = normalizeSecret(rawProxy.secret());

        if (host == null || host.isBlank()) {
            return Optional.empty();
        }

        if (port == null || port < 1 || port > 65535) {
            return Optional.empty();
        }

        if (rawProxy.type() == ProxyType.MTPROTO && (secret == null || secret.isBlank())) {
            return Optional.empty();
        }

        return Optional.of(
                RawProxy.builder()
                        .host(host)
                        .port(port)
                        .secret(secret)
                        .type(rawProxy.type())
                        .source(rawProxy.source())
                        .build()
        );
    }

    private String normalizeHost(String host) {
        if (host == null) {
            return null;
        }

        String normalized = host.trim();
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeSecret(String secret) {
        if (secret == null) {
            return null;
        }

        String normalized = secret.trim().replace(" ", "");
        if (normalized.isEmpty()) {
            return normalized;
        }

        if (isHexLike(normalized)) {
            return normalized.toLowerCase(Locale.ROOT);
        }

        return stripTrailingPadding(normalized);
    }

    private boolean isHexLike(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private String stripTrailingPadding(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '=') {
            end--;
        }
        return value.substring(0, end);
    }
}
