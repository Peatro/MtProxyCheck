package com.peatroxd.mtprototest.admin.dto;

import java.util.List;

public record AdminCatalogOverviewResponse(
        long totalProxies,
        long newCount,
        long aliveCount,
        long deadCount,
        long archivedCount,
        long verifiedCount,
        long quickOkCount,
        long unverifiedCount,
        long whitelistedCount,
        long blacklistedCount,
        List<AdminSourceOverviewResponse> sources
) {
}
