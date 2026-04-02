package com.peatroxd.mtprototest.proxy.dto.request;

import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;
import jakarta.validation.constraints.NotNull;

public record ProxyFeedbackRequest(
        @NotNull(message = "Feedback result is required")
        ProxyFeedbackResult result,
        ProxyFeedbackPlatform platform
) {
}
