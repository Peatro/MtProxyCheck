package com.peatroxd.mtprototest.proxy.controller;

import com.peatroxd.mtprototest.proxy.dto.request.ProxyListRequest;
import com.peatroxd.mtprototest.proxy.dto.request.ProxyFeedbackRequest;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyFeedbackResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyPageResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyResponse;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyStatsResponse;
import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;
import com.peatroxd.mtprototest.proxy.service.ProxyFeedbackService;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proxies")
@RequiredArgsConstructor
public class ProxyController {

    private final ProxyService proxyService;
    private final ProxyFeedbackService proxyFeedbackService;

    @GetMapping
    public ProxyPageResponse getProxies(
            @RequestParam(required = false) ProxyStatus status,
            @RequestParam(required = false) ProxyVerificationStatus verificationStatus,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Long maxLatency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        return proxyService.getProxies(new ProxyListRequest(
                status,
                verificationStatus,
                minScore,
                maxLatency,
                page,
                size,
                sortBy,
                sortDirection
        ));
    }

    @GetMapping("/stats")
    public ProxyStatsResponse getStats() {
        return proxyService.getStats();
    }

    @GetMapping("/{proxyId}")
    public ProxyResponse getById(@PathVariable Long proxyId) {
        return proxyService.getById(proxyId);
    }

    @GetMapping("/best")
    public List<ProxyResponse> best() {
        return proxyService.getBest();
    }

    @PostMapping("/{proxyId}/feedback")
    public ProxyFeedbackResponse submitFeedback(
            @PathVariable Long proxyId,
            @Valid @RequestBody ProxyFeedbackRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return proxyFeedbackService.submitFeedback(proxyId, request, extractClientKey(httpServletRequest));
    }

    private String extractClientKey(HttpServletRequest request) {
        String fingerprint = request.getHeader("X-Client-Fingerprint");
        if (fingerprint != null && !fingerprint.isBlank()) {
            return fingerprint;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
