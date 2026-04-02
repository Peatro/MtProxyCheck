package com.peatroxd.mtprototest.checker.model;

import com.peatroxd.mtprototest.checker.enums.ProxyCheckType;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;

import java.time.LocalDateTime;

public record ProxyCheckHistoryRecord(
        LocalDateTime checkedAt,
        ProxyCheckType checkType,
        boolean alive,
        ProxyVerificationStatus verificationStatus,
        Long latencyMs,
        MtProtoProbeFailureCode failureCode,
        String failureReason
) {
}
