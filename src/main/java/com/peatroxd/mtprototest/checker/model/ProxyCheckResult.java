package com.peatroxd.mtprototest.checker.model;

import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;

public record ProxyCheckResult(
        boolean alive,
        long latencyMs,
        ProxyVerificationStatus verificationStatus,
        MtProtoProbeFailureCode failureCode,
        String errorMessage
) {
}
