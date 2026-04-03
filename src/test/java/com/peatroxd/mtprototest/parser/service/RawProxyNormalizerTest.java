package com.peatroxd.mtprototest.parser.service;

import com.peatroxd.mtprototest.parser.model.RawProxy;
import com.peatroxd.mtprototest.parser.model.RawProxyRejectReason;
import com.peatroxd.mtprototest.proxy.enums.ProxyType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RawProxyNormalizerTest {

    private final RawProxyNormalizer normalizer = new RawProxyNormalizer();

    @Test
    void shouldNormalizeHostSecretAndKeepValidProxy() {
        RawProxy rawProxy = RawProxy.builder()
                .host(" Example.com. ")
                .port(443)
                .secret(" ABCDEF ")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        RawProxy normalized = normalizer.normalize(rawProxy).orElseThrow();

        assertThat(normalized.host()).isEqualTo("example.com");
        assertThat(normalized.secret()).isEqualTo("abcdef");
        assertThat(normalized.port()).isEqualTo(443);
    }

    @Test
    void shouldNormalizeIpv6BracketsAndRemoveBase64Padding() {
        RawProxy rawProxy = RawProxy.builder()
                .host(" [2001:db8::1]. ")
                .port(443)
                .secret(" FgMBAgABAAH8AwOG4kw63Q== ")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        RawProxy normalized = normalizer.normalize(rawProxy).orElseThrow();

        assertThat(normalized.host()).isEqualTo("2001:db8::1");
        assertThat(normalized.secret()).isEqualTo("FgMBAgABAAH8AwOG4kw63Q");
    }

    @Test
    void shouldTreatEquivalentSecretsFromDifferentFeedsTheSameAfterNormalization() {
        RawProxy first = normalizer.normalize(RawProxy.builder()
                .host("Example.com")
                .port(443)
                .secret("FgMBAgABAAH8AwOG4kw63Q==")
                .type(ProxyType.MTPROTO)
                .source("feed_a")
                .build()).orElseThrow();

        RawProxy second = normalizer.normalize(RawProxy.builder()
                .host("example.com.")
                .port(443)
                .secret("FgMBAgABAAH8AwOG4kw63Q")
                .type(ProxyType.MTPROTO)
                .source("feed_b")
                .build()).orElseThrow();

        assertThat(first.host()).isEqualTo(second.host());
        assertThat(first.port()).isEqualTo(second.port());
        assertThat(first.secret()).isEqualTo(second.secret());
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
    void shouldExposeInvalidPortRejectReason() {
        RawProxy rawProxy = RawProxy.builder()
                .host("example.com")
                .port(70000)
                .secret("secret")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        var result = normalizer.normalizeWithReason(rawProxy);

        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(RawProxyRejectReason.INVALID_PORT);
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

    @Test
    void shouldExposeEmptySecretRejectReason() {
        RawProxy rawProxy = RawProxy.builder()
                .host("example.com")
                .port(443)
                .secret(" ")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        var result = normalizer.normalizeWithReason(rawProxy);

        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(RawProxyRejectReason.EMPTY_SECRET);
    }

    @Test
    void shouldExposeEmptyHostRejectReason() {
        RawProxy rawProxy = RawProxy.builder()
                .host(" . ")
                .port(443)
                .secret("abcdef")
                .type(ProxyType.MTPROTO)
                .source("test")
                .build();

        var result = normalizer.normalizeWithReason(rawProxy);

        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(RawProxyRejectReason.EMPTY_HOST);
    }

    @Test
    void shouldExposeNullInputRejectReason() {
        var result = normalizer.normalizeWithReason(null);

        assertThat(result.accepted()).isFalse();
        assertThat(result.rejectReason()).isEqualTo(RawProxyRejectReason.NULL_INPUT);
    }
}
