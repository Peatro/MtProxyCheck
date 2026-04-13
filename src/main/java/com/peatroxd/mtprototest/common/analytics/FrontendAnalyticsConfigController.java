package com.peatroxd.mtprototest.common.analytics;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/frontend-config", "/api/v1/frontend-config"})
@EnableConfigurationProperties(PostHogProperties.class)
public class FrontendAnalyticsConfigController {

    private final PostHogProperties postHogProperties;

    public FrontendAnalyticsConfigController(PostHogProperties postHogProperties) {
        this.postHogProperties = postHogProperties;
    }

    @GetMapping
    public FrontendAnalyticsConfigResponse getFrontendConfig() {
        boolean enabled = postHogProperties.isEnabled() && StringUtils.hasText(postHogProperties.getApiKey());

        return new FrontendAnalyticsConfigResponse(
                enabled,
                enabled ? postHogProperties.getApiKey() : null,
                enabled ? postHogProperties.getHost() : null
        );
    }
}
