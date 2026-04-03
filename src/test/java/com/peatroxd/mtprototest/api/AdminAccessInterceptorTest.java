package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.web.AdminAccessInterceptor;
import com.peatroxd.mtprototest.common.web.AdminAccessProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccessInterceptorTest {

    @Test
    void shouldRejectProtectedEndpointsWithoutAdminKey() {
        AdminAccessInterceptor interceptor = interceptor(true, "test-admin-key");

        assertThatThrownBy(() -> interceptor.preHandle(postRequest("/api/import/proxies", null), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void shouldAllowProtectedEndpointsWithValidAdminKey() {
        AdminAccessInterceptor interceptor = interceptor(true, "test-admin-key");

        assertThatCode(() -> interceptor.preHandle(postRequest("/api/v1/check/proxies", "test-admin-key"), new MockHttpServletResponse(), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldIgnorePublicEndpoints() {
        AdminAccessInterceptor interceptor = interceptor(true, "test-admin-key");

        assertThatCode(() -> interceptor.preHandle(getRequest("/api/proxies/best"), new MockHttpServletResponse(), new Object()))
                .doesNotThrowAnyException();
    }

    private AdminAccessInterceptor interceptor(boolean enabled, String key) {
        AdminAccessProperties properties = new AdminAccessProperties();
        properties.setEnabled(enabled);
        properties.setHeaderName("X-Admin-Key");
        properties.setKey(key);
        return new AdminAccessInterceptor(properties);
    }

    private MockHttpServletRequest postRequest(String path, String adminKey) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        if (adminKey != null) {
            request.addHeader("X-Admin-Key", adminKey);
        }
        return request;
    }

    private MockHttpServletRequest getRequest(String path) {
        return new MockHttpServletRequest("GET", path);
    }
}
