package com.peatroxd.mtprototest.proxy.dto.response;

public record RecentCheckSummaryResponse(
        long totalChecks,
        long successfulChecks,
        long failedChecks,
        long deepSuccesses,
        long deepFailures
) {
}
