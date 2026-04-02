package com.peatroxd.mtprototest.proxy.dto.request;

import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackPlatform;
import com.peatroxd.mtprototest.proxy.enums.ProxyFeedbackResult;

public record ProxyFeedbackRequest(
        ProxyFeedbackResult result,
        ProxyFeedbackPlatform platform
) {
}
