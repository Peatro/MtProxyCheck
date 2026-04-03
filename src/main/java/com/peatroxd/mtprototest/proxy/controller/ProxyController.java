package com.peatroxd.mtprototest.proxy.controller;

import com.peatroxd.mtprototest.common.web.ClientRequestKeyResolver;
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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Validated
@RequestMapping({"/api/proxies", "/api/v1/proxies"})
@RequiredArgsConstructor
public class ProxyController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProxyService proxyService;
    private final ProxyFeedbackService proxyFeedbackService;
    private final ClientRequestKeyResolver clientRequestKeyResolver;

    @GetMapping
    public ProxyPageResponse getProxies(
            @RequestParam(required = false) ProxyStatus status,
            @RequestParam(required = false) ProxyVerificationStatus verificationStatus,
            @RequestParam(required = false) @Min(0) @Max(100) Integer minScore,
            @RequestParam(required = false) @Min(0) Long maxLatency,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        validateListRequest(minScore, maxLatency, page, size);
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
        return proxyFeedbackService.submitFeedback(proxyId, request, clientRequestKeyResolver.resolve(httpServletRequest));
    }

    private void validateListRequest(Integer minScore, Long maxLatency, int page, int size) {
        if (minScore != null && (minScore < 0 || minScore > 100)) {
            throw new ResponseStatusException(BAD_REQUEST, "minScore must be between 0 and 100");
        }
        if (maxLatency != null && maxLatency < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "maxLatency must be greater than or equal to 0");
        }
        if (page < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(BAD_REQUEST, "size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
