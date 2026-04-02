package com.peatroxd.mtprototest.proxy.controller;

import com.peatroxd.mtprototest.proxy.dto.request.ProxyFeedbackRequest;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyFeedbackResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.service.ProxyFeedbackService;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proxies")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;
    private final ProxyFeedbackService proxyFeedbackService;

    @GetMapping("/best")
    public List<ProxyResponse> best() {
        return proxyService.getBest();
    }

    @PostMapping("/{proxyId}/feedback")
    public ProxyFeedbackResponse submitFeedback(
            @PathVariable Long proxyId,
            @RequestBody ProxyFeedbackRequest request
    ) {
        return proxyFeedbackService.submitFeedback(proxyId, request);
    }
}
