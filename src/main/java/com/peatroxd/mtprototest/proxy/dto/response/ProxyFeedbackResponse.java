package com.peatroxd.mtprototest.proxy.dto.response;

public record ProxyFeedbackResponse(
        boolean success,
        Long proxyId,
        String result,
        String platform
) {
}
