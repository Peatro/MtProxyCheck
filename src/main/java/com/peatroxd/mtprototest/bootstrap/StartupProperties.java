package com.peatroxd.mtprototest.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.startup")
public class StartupProperties {

    private boolean bootstrapEnabled = true;
    private int checkBatchSize = 120;
    private int deepProbeLimit = 10;

}
