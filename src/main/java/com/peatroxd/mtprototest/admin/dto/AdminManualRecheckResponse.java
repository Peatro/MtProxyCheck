package com.peatroxd.mtprototest.admin.dto;

public record AdminManualRecheckResponse(
        Long proxyId,
        String status,
        String verificationStatus,
        Long latencyMs,
        String failureCode,
        String failureReason
) {
}
