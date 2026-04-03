package com.peatroxd.mtprototest.admin.dto;

import java.time.LocalDateTime;

public record AdminSourceOverviewResponse(
        String source,
        long totalProxies,
        long aliveCount,
        long verifiedCount,
        long blacklistedCount,
        LocalDateTime lastImportStartedAt,
        LocalDateTime lastImportCompletedAt,
        boolean lastImportSucceeded,
        int lastImported,
        int lastSkipped,
        int lastRejected,
        String lastError
) {
}
