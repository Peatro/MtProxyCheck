package com.peatroxd.mtprototest.parser.service;

import com.peatroxd.mtprototest.parser.model.RawProxy;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RawProxyNormalizerTest {

    private final RawProxyNormalizer normalizer = new RawProxyNormalizer();

    @Test
    void shouldNormalizeHostSecretAndKeepValidProxy() {
        RawProxy rawProxy = RawProxy.builder()
                .host(" example.com. ")
                .port(443)
                .secret(" abcdef ")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        RawProxy normalized = normalizer.normalize(rawProxy).orElseThrow();

        assertThat(normalized.host()).isEqualTo("example.com");
        assertThat(normalized.secret()).isEqualTo("abcdef");
        assertThat(normalized.port()).isEqualTo(443);
    }

    @Test
    void shouldRejectOutOfRangePort() {
        RawProxy rawProxy = RawProxy.builder()
                .host("example.com")
                .port(70000)
                .secret("secret")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        assertThat(normalizer.normalize(rawProxy)).isEmpty();
    }

    @Test
    void shouldRejectMtprotoWithoutSecret() {
        RawProxy rawProxy = RawProxy.builder()
                .host("example.com")
                .port(443)
                .secret(" ")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        assertThat(normalizer.normalize(rawProxy)).isEmpty();
    }
}
