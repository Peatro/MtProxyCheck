package com.peatroxd.mtprototest.common.analytics;

public record FrontendAnalyticsConfigResponse(
        boolean posthogEnabled,
        String posthogApiKey,
        String posthogHost
) {
}
