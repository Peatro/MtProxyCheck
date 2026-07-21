package com.peatroxd.mtprototest.proxy.controller;

import com.peatroxd.mtprototest.checker.client.ProxyPingAgentClient;
import com.peatroxd.mtprototest.proxy.dto.response.HealthStatusResponse;
import com.peatroxd.mtprototest.proxy.repository.ProxyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping({"/api/public/health-status", "/api/v1/health-status"})
@RequiredArgsConstructor
public class HealthStatusController {

    private final ProxyRepository proxyRepository;
    private final ProxyPingAgentClient agentClient;

    @Value("${app.checker.freshness-threshold-seconds:600}")
    private long freshnessThresholdSeconds;

    @GetMapping
    public HealthStatusResponse healthStatus() {
        LocalDateTime latest = proxyRepository.findMaxLastCheckedAt();
        Long ageSeconds = latest != null
                ? Duration.between(latest, LocalDateTime.now()).getSeconds()
                : null;

        ProxyPingAgentClient.EgressInfo egress = agentClient.egress();
        String egressCountry = egress != null ? egress.country() : null;

        boolean fresh = ageSeconds != null && ageSeconds <= freshnessThresholdSeconds;
        boolean egressOk = "RU".equals(egressCountry);
        boolean healthy = fresh && egressOk;

        return new HealthStatusResponse(healthy, latest, ageSeconds, egressCountry);
    }
}
