package com.peatroxd.mtprototest.common.analytics;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.analytics.posthog")
public class PostHogProperties {

    private boolean enabled = false;
    private String apiKey;
    private String host = "https://us.i.posthog.com";
}
