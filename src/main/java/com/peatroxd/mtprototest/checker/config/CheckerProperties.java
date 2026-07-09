package com.peatroxd.mtprototest.checker.config;

import lombok.Data;
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
    private long archiveDeadAfterMs = 604_800_000;
    private int archiveMinConsecutiveFailures = 8;
    private Socks socks = new Socks();

    @Data
    public static class Socks {
        private boolean enabled = false;
        private String host = "127.0.0.1";
        private int port = 1080;
    }
}
