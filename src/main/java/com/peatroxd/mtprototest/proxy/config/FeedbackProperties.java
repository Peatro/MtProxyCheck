package com.peatroxd.mtprototest.proxy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.feedback")
public class FeedbackProperties {

    private long dedupeWindowMinutes = 60;
    private int recentLimit = 20;

}
