package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.web.ClientRequestKeyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientRequestKeyResolverTest {

    private final ClientRequestKeyResolver resolver = new ClientRequestKeyResolver();

    @Test
    void shouldPreferExplicitClientFingerprint() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Client-Fingerprint", "fingerprint-1");
        request.addHeader("X-Forwarded-For", "198.51.100.10");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("fingerprint-1");
    }

    @Test
    void shouldUseFirstForwardedForAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.10, 203.0.113.7");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.10");
    }

    @Test
    void shouldFallbackToRealIpBeforeRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.11");
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.11");
    }

    @Test
    void shouldFallbackToRemoteAddressWhenHeadersAreMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }
}
