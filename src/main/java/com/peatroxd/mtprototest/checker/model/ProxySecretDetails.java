package com.peatroxd.mtprototest.checker.model;

public record ProxySecretDetails(
        ProxySecretType type,
        String normalizedHex,
        byte[] keyBytes,
        boolean supported,
        String message
) {
}
