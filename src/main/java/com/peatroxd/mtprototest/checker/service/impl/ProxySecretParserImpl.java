package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.model.ProxySecretDetails;
import com.peatroxd.mtprototest.checker.model.ProxySecretType;
import com.peatroxd.mtprototest.checker.service.ProxySecretParser;
import org.springframework.stereotype.Component;

import java.util.HexFormat;

@Component
public class ProxySecretParserImpl implements ProxySecretParser {

    private static final byte PADDED_INTERMEDIATE_PREFIX = (byte) 0xDD;
    private static final byte FAKE_TLS_PREFIX = (byte) 0xEE;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    @Override
    public ProxySecretDetails parse(String secretHex) {
        if (secretHex == null || secretHex.isBlank()) {
            return new ProxySecretDetails(ProxySecretType.UNKNOWN, null, null, false, "Proxy secret is empty");
        }

        String normalizedHex = secretHex.trim().toLowerCase();
        byte[] rawSecret;

        try {
            rawSecret = HEX_FORMAT.parseHex(normalizedHex);
        } catch (IllegalArgumentException e) {
            return new ProxySecretDetails(ProxySecretType.UNKNOWN, normalizedHex, null, false, "Proxy secret is not valid hex");
        }

        if (rawSecret.length == 16) {
            return new ProxySecretDetails(
                    ProxySecretType.STANDARD,
                    normalizedHex,
                    rawSecret,
                    true,
                    "Standard 16-byte MTProxy secret"
            );
        }

        if (rawSecret.length == 17 && rawSecret[0] == PADDED_INTERMEDIATE_PREFIX) {
            return new ProxySecretDetails(
                    ProxySecretType.PADDED_INTERMEDIATE,
                    normalizedHex,
                    slice(rawSecret, 1, rawSecret.length),
                    true,
                    "dd-prefixed padded intermediate MTProxy secret"
            );
        }

        if (rawSecret.length >= 17 && rawSecret[0] == FAKE_TLS_PREFIX) {
            return new ProxySecretDetails(
                    ProxySecretType.FAKE_TLS,
                    normalizedHex,
                    null,
                    false,
                    "ee-prefixed fake TLS secrets are recognized but not supported by deep probe yet"
            );
        }

        return new ProxySecretDetails(
                ProxySecretType.UNKNOWN,
                normalizedHex,
                null,
                false,
                "Unsupported MTProxy secret format"
        );
    }

    private byte[] slice(byte[] value, int fromInclusive, int toExclusive) {
        byte[] result = new byte[toExclusive - fromInclusive];
        System.arraycopy(value, fromInclusive, result, 0, result.length);
        return result;
    }
}
