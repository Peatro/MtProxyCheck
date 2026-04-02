package com.peatroxd.mtprototest.proxy.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProxyResponse(
        Long id,
        String host,
        Integer port,
        String secret,
        String type,
        String source,
        String status,
        String verificationStatus,
        boolean verified,
        Integer score,
        Long lastLatencyMs,
        LocalDateTime lastCheckedAt,
        LocalDateTime lastSuccessAt,
        Integer consecutiveFailures,
        Integer consecutiveSuccesses,
        String telegramDeepLink,
        String telegramWebLink
) {
}
