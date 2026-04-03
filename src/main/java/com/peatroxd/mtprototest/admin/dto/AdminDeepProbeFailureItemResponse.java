package com.peatroxd.mtprototest.admin.dto;

import java.time.LocalDateTime;

public record AdminDeepProbeFailureItemResponse(
        Long proxyId,
        String host,
        Integer port,
        String source,
        String failureCode,
        String failureReason,
        LocalDateTime checkedAt
) {
}
