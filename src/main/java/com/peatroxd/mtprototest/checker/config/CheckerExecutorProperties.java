package com.peatroxd.mtprototest.checker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.checker.executor")
public class CheckerExecutorProperties {

    private int corePoolSize = 16;
    private int maxPoolSize = 32;
    private int queueCapacity = 200;
    private int awaitTerminationSeconds = 30;
    private String threadNamePrefix = "proxy-checker-";

}
