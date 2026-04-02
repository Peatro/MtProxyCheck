package com.peatroxd.mtprototest.bootstrap;

import com.peatroxd.mtprototest.checker.model.ProxyBatchCheckSummary;
import com.peatroxd.mtprototest.checker.service.ProxyBatchCheckService;
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

    @Override
    public void run(@NonNull ApplicationArguments args) {
        log.info("Starting proxy bootstrap on application startup");

        proxyImportService.importAll();

        ProxyBatchCheckSummary summary = proxyBatchCheckService.checkAllProxies();
        log.info(
                "Startup proxy bootstrap finished: total={}, quickOk={}, verified={}, dead={}",
                summary.totalChecked(),
                summary.quickOkCount(),
                summary.verifiedCount(),
                summary.deadCount()
        );
    }
}
