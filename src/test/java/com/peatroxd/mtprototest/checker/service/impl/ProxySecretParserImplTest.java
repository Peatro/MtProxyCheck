package com.peatroxd.mtprototest.checker.service.impl;

import com.peatroxd.mtprototest.checker.model.ProxySecretType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProxySecretParserImplTest {

    private final ProxySecretParserImpl parser = new ProxySecretParserImpl();

    @Test
    void shouldParseStandardSecret() {
        var details = parser.parse("00112233445566778899aabbccddeeff");

        assertThat(details.supported()).isTrue();
        assertThat(details.type()).isEqualTo(ProxySecretType.STANDARD);
        assertThat(details.keyBytes()).hasSize(16);
    }

    @Test
    void shouldParseDdPrefixedSecret() {
        var details = parser.parse("dd00112233445566778899aabbccddeeff");

        assertThat(details.supported()).isTrue();
        assertThat(details.type()).isEqualTo(ProxySecretType.PADDED_INTERMEDIATE);
        assertThat(details.keyBytes()).hasSize(16);
    }

    @Test
    void shouldParseEeSecretWithKeyAndDomain() {
        // ee + 16-byte key + domain "google.com" (UTF-8 hex)
        var details = parser.parse("ee00112233445566778899aabbccddeeff676f6f676c652e636f6d");

        assertThat(details.supported()).isTrue();
        assertThat(details.type()).isEqualTo(ProxySecretType.FAKE_TLS);
        // keyBytes = 16-byte key + domain, ee stripped
        assertThat(details.keyBytes()).hasSize(16 + "google.com".length());
    }

    @Test
    void shouldRejectInvalidHex() {
        var details = parser.parse("not-hex");

        assertThat(details.supported()).isFalse();
        assertThat(details.type()).isEqualTo(ProxySecretType.UNKNOWN);
    }
}
