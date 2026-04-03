package com.peatroxd.mtprototest.bootstrap;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
import com.peatroxd.mtprototest.checker.service.ProxyCheckRunCoordinator;
import com.peatroxd.mtprototest.parser.service.ProxyImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.startup", name = "bootstrap-enabled", havingValue = "true", matchIfMissing = true)
public class ProxyStartupBootstrap implements ApplicationRunner {

    private final ProxyImportService proxyImportService;
    private final ProxyBatchCheckService proxyBatchCheckService;
    private final ProxyCheckRunCoordinator proxyCheckRunCoordinator;
    private final StartupProperties startupProperties;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        log.info("Starting proxy bootstrap on application startup");

        proxyImportService.importAll();

        if (!proxyCheckRunCoordinator.tryStartCatalogCycle("startup proxy bootstrap")) {
            return;
        }

        try {
            ProxyBatchCheckSummary summary = proxyBatchCheckService.checkStartupProxies(
                    startupProperties.getCheckBatchSize(),
                    startupProperties.getDeepProbeLimit()
            );
            log.info(
                    "Startup proxy bootstrap finished: total={}, quickOk={}, verified={}, dead={}, batchSize={}, deepProbeLimit={}",
                    summary.totalChecked(),
                    summary.quickOkCount(),
                    summary.verifiedCount(),
                    summary.deadCount(),
                    startupProperties.getCheckBatchSize(),
                    startupProperties.getDeepProbeLimit()
            );
        } finally {
            proxyCheckRunCoordinator.finishCatalogCycle();
        }
    }
}
