package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.api.ApiErrorResponse;
import com.peatroxd.mtprototest.common.api.GlobalApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

class GlobalApiExceptionHandlerTest {

    private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

    @Test
    void shouldBuildStructuredNotFoundErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/proxies/42");

        ApiErrorResponse response = handler.handleResponseStatusException(
                new ResponseStatusException(NOT_FOUND, "Proxy not found"),
                request
        );

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.code()).isEqualTo("NOT_FOUND");
        assertThat(response.message()).isEqualTo("Proxy not found");
        assertThat(response.path()).isEqualTo("/api/proxies/42");
    }

    @Test
    void shouldBuildStructuredRateLimitErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/proxies/1/feedback");

        ApiErrorResponse response = handler.handleResponseStatusException(
                new ResponseStatusException(TOO_MANY_REQUESTS, "Rate limit exceeded"),
                request
        );

        assertThat(response.status()).isEqualTo(429);
        assertThat(response.code()).isEqualTo("RATE_LIMITED");
        assertThat(response.message()).isEqualTo("Rate limit exceeded");
        assertThat(response.path()).isEqualTo("/api/proxies/1/feedback");
    }

    @Test
    void shouldBuildStructuredBadRequestResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/proxies");

        ApiErrorResponse response = handler.handleBadRequest(
                new IllegalArgumentException("Bad request"),
                request
        );

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.code()).isEqualTo("BAD_REQUEST");
        assertThat(response.message()).isEqualTo("Bad request");
        assertThat(response.path()).isEqualTo("/api/proxies");
    }
}
