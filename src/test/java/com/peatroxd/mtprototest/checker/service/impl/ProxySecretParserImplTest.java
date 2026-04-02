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
    void shouldClassifyEeSecretAsUnsupported() {
        var details = parser.parse("ee00112233445566778899aabbccddeeff");

        assertThat(details.supported()).isFalse();
        assertThat(details.type()).isEqualTo(ProxySecretType.FAKE_TLS);
        assertThat(details.message()).contains("not supported");
    }

    @Test
    void shouldRejectInvalidHex() {
        var details = parser.parse("not-hex");

        assertThat(details.supported()).isFalse();
        assertThat(details.type()).isEqualTo(ProxySecretType.UNKNOWN);
    }
}
