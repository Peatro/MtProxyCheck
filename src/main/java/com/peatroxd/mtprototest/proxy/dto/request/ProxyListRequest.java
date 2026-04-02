package com.peatroxd.mtprototest.proxy.dto.request;

import com.peatroxd.mtprototest.proxy.enums.ProxyStatus;
import com.peatroxd.mtprototest.proxy.enums.ProxyVerificationStatus;

public record ProxyListRequest(
        ProxyStatus status,
        ProxyVerificationStatus verificationStatus,
        Integer minScore,
        Long maxLatency,
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
}
