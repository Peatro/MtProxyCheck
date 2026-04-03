package com.peatroxd.mtprototest.admin.dto;

import java.util.List;

public record AdminDeepProbeFailuresResponse(
        List<AdminDeepProbeFailureSummaryResponse> summary,
        List<AdminDeepProbeFailureItemResponse> recent
) {
}
