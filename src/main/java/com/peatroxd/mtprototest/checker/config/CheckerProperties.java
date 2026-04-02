package com.peatroxd.mtprototest.checker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.checker")
public class CheckerProperties {

    private long initialDelayMs = 300_000;
    private long fixedDelayMs = 300_000;
    private int batchSize = 200;
    private int deepProbeLimit = 20;
    private long aliveQuickOkRecheckAfterMs = 300_000;
    private long aliveVerifiedRecheckAfterMs = 1_800_000;
    private long deadRetryAfterMs = 21_600_000;

}
