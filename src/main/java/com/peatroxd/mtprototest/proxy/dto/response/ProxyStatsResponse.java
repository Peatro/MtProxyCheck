package com.peatroxd.mtprototest.proxy.dto.response;

public record ProxyStatsResponse(
        long totalProxies,
        long newCount,
        long aliveCount,
        long deadCount,
        long verifiedCount,
        long quickOkCount,
        long unverifiedCount,
        RecentCheckSummaryResponse recentChecks
) {
}
