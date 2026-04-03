package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.web.ClientRequestKeyResolver;
import com.peatroxd.mtprototest.proxy.controller.ProxyController;
import com.peatroxd.mtprototest.proxy.service.ProxyFeedbackService;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProxyControllerValidationTest {

    private final ProxyController controller = new ProxyController(
            Mockito.mock(ProxyService.class),
            Mockito.mock(ProxyFeedbackService.class),
            new ClientRequestKeyResolver()
    );

    @Test
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> controller.getProxies(null, null, null, null, -1, 20, "score", "desc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("page must be greater than or equal to 0");
    }

    @Test
    void shouldRejectOversizedPageSize() {
        assertThatThrownBy(() -> controller.getProxies(null, null, null, null, 0, 101, "score", "desc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("size must be between 1 and 100");
    }

    @Test
    void shouldRejectInvalidMinScore() {
        assertThatThrownBy(() -> controller.getProxies(null, null, 101, null, 0, 20, "score", "desc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("minScore must be between 0 and 100");
    }
}
