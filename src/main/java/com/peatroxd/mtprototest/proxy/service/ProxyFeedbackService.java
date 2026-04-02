package com.peatroxd.mtprototest.proxy.service;

import com.peatroxd.mtprototest.proxy.dto.request.ProxyFeedbackRequest;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyFeedbackResponse;

public interface ProxyFeedbackService {
    ProxyFeedbackResponse submitFeedback(Long proxyId, ProxyFeedbackRequest request, String clientKey);
}
