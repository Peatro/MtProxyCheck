package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.web.ClientRequestKeyResolver;
import com.peatroxd.mtprototest.common.web.RateLimitInterceptor;
import com.peatroxd.mtprototest.common.web.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitInterceptorTest {

    @Test
    void shouldRateLimitPublicReadRequests() throws Exception {
        RateLimitInterceptor interceptor = interceptor(2, 1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatCode(() -> interceptor.preHandle(getRequest("/api/proxies/best", "reader-1"), response, new Object()))
                .doesNotThrowAnyException();
        assertThatCode(() -> interceptor.preHandle(getRequest("/api/proxies/best", "reader-1"), response, new Object()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> interceptor.preHandle(getRequest("/api/proxies/best", "reader-1"), response, new Object()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("429");
    }

    @Test
    void shouldRateLimitFeedbackRequests() throws Exception {
        RateLimitInterceptor interceptor = interceptor(10, 1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatCode(() -> interceptor.preHandle(postRequest("/api/proxies/1/feedback", "writer-1"), response, new Object()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> interceptor.preHandle(postRequest("/api/proxies/1/feedback", "writer-1"), response, new Object()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("429");
    }

    private RateLimitInterceptor interceptor(int publicReadLimit, int feedbackWriteLimit) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setPublicReadLimit(publicReadLimit);
        properties.setPublicReadWindowMs(60_000);
        properties.setFeedbackWriteLimit(feedbackWriteLimit);
        properties.setFeedbackWriteWindowMs(60_000);
        return new RateLimitInterceptor(new ClientRequestKeyResolver(), properties);
    }

    private MockHttpServletRequest getRequest(String path, String fingerprint) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader("X-Client-Fingerprint", fingerprint);
        return request;
    }

    private MockHttpServletRequest postRequest(String path, String fingerprint) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.addHeader("X-Client-Fingerprint", fingerprint);
        return request;
    }
}
