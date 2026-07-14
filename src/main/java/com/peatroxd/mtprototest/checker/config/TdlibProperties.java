package com.peatroxd.mtprototest.checker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.checker.tdlib")
public class TdlibProperties {

    private String agentUrl = "http://127.0.0.1:8090";
    private long interval = 60_000;
    private int batchLimit = 50;
    private int concurrency = 8;
    private long recheckAfterMs = 1_800_000;
    private int requestTimeoutMs = 20_000;
    private int healthTimeoutMs = 2_500;
}
