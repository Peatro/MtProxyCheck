package com.peatroxd.mtprototest.checker.controller;

import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/check", "/api/v1/check"})
@RequiredArgsConstructor
public class ProxyCheckController {

    private final ProxyBatchCheckService proxyBatchCheckService;

    @PostMapping("/proxies")
    public ResponseEntity<String> checkProxies() {
        proxyBatchCheckService.checkNewProxies();
        proxyBatchCheckService.checkAliveQuickOkProxies();
        proxyBatchCheckService.checkAliveVerifiedProxies();
        proxyBatchCheckService.checkDeadProxies();
        return ResponseEntity.ok("Lifecycle proxy check completed");
    }
}
