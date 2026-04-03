package com.peatroxd.mtprototest.admin.service;

import java.time.LocalDateTime;

public record SourceImportSnapshot(
        String source,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        boolean succeeded,
        int imported,
        int skipped,
        int rejected,
        String errorMessage
) {
}
